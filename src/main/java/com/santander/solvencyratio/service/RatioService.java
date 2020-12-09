package com.santander.solvencyratio.service;

import com.santander.digital.verifiedid.impl.VerifiedIdClientImp;
import com.santander.digital.verifiedid.model.TokenRequest;
import com.santander.digital.verifiedid.model.claims.sharing.Claims;
import com.santander.digital.verifiedid.model.claims.verifying.AssertionClaims;
import com.santander.digital.verifiedid.model.claims.verifying.Balance;
import com.santander.digital.verifiedid.model.init.authorize.InitiateAuthorizeRequest;
import com.santander.digital.verifiedid.model.init.authorize.InitiateAuthorizeResponse;
import com.santander.digital.verifiedid.model.token.IdToken;
import com.santander.solvencyratio.config.RatioProperties;
import com.santander.solvencyratio.repository.RouteRepository;
import com.santander.solvencyratio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Currency;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatioService {

    private final VerifiedIdClientImp verifiedIdClient;
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final RatioProperties properties;

    public String authorize(final String passport, String callback){

        final Claims idClaims = new Claims();
        idClaims.passportId().withEssential(true);

        InitiateAuthorizeRequest request = InitiateAuthorizeRequest.builder()
                .redirectUri(properties.getRedirectUri())
                .claims(idClaims)
                .assertionClaims(generateAssertionClaims(passport))
                .state(callback)
                .purpose("To check your financial health, we need to validate some details")
                .build();

        InitiateAuthorizeResponse initiateAuthorizeResponse = verifiedIdClient.initiateAuthorize(request);
        routeRepository.put(initiateAuthorizeResponse.getNonce(), callback);
        log.info("Route added: Nonce {}, Callback: {}", initiateAuthorizeResponse.getNonce(), callback);
        return  initiateAuthorizeResponse.getRedirectionUri();
    }

    public String verify(String code) {
        TokenRequest tokenRequest = TokenRequest.builder()
                .redirectUri(properties.getRedirectUri())
                .authorizationCode(code)
                .build();

        IdToken token = verifiedIdClient.token(tokenRequest);

        try {
            double ratio = calculateRatio(token);
            userRepository.put(token.getPassportId(), ratio);
            log.info("User added: Passport {}, Ratio: {}", token.getPassportId(), ratio);
        } catch (IdentityNotVerified ignored) {
            log.info("Passport not verified, ignoring user {}", token.getPassportId());
        }

        return routeRepository.get(token.getNonce());
    }

    private double calculateRatio(IdToken token) throws IdentityNotVerified{

        if(!token.getAssertionClaims().get("passport_id").getResult())
            throw  new IdentityNotVerified();

        double ratio = 0;
        double tick = 0.25;
        if(isValidClaim(token, "total_balance")) ratio+= tick;
        if(isValidClaim(token,  "last_year_money_in")) ratio+= tick;
        if(isValidClaim(token, "last_quarter_money_in")) ratio+= tick;
        if(isValidClaim(token, "average_monthly_money_in")) ratio+= tick;
        return ratio;
    }

    private boolean isValidClaim(IdToken token, String label){
        return token.getAssertionClaims().get(label).getResult();
    }

    private AssertionClaims generateAssertionClaims(final String passport){
        final AssertionClaims assertionClaims = new AssertionClaims();
        assertionClaims.passportId().eq(passport);

        assertionClaims.totalBalance() //Total amount of money hold on user's bank accounts
                .withAssertion(Balance.amount().gte(BigDecimal.valueOf(5000)))
                .withAssertion(Balance.currency().eq(Currency.getInstance("EUR")));

        assertionClaims.averageMonthlyMoneyIn() //Average monthly amount of money incoming to user bank account for the last year
                .withAssertion(Balance.amount().gte(BigDecimal.valueOf(2000)))
                .withAssertion(Balance.currency().eq(Currency.getInstance("EUR")));

        assertionClaims.lastYearMoneyIn()//Total amount of money incoming to user bank account for the last year
                .withAssertion(Balance.amount().gte(BigDecimal.valueOf(24000)))
                .withAssertion(Balance.currency().eq(Currency.getInstance("EUR")));

        assertionClaims.lastQuarterMoneyIn()//Total amount of money incoming to user bank account for the last three months
                .withAssertion(Balance.amount().gte(BigDecimal.valueOf(6000)))
                .withAssertion(Balance.currency().eq(Currency.getInstance("EUR")));
        return assertionClaims;
    }

}
