package com.santander.solvencyratio.controller;

import com.santander.solvencyratio.repository.UserRepository;
import com.santander.solvencyratio.service.RatioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
public class RatioController {

    private final RatioService ratioService;
    private final UserRepository userRepository;

    @CrossOrigin(origins = "*")
    @RequestMapping(method = RequestMethod.GET, path = "/solvency/ratio/verify/{passport}")
    public ResponseEntity<?> getRatio(@PathVariable String passport){
        if(userRepository.containsKey(passport))
            return ResponseEntity.ok(userRepository.get(passport));
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(method = RequestMethod.GET, path = "/solvency/ratio/create/{passport}")
    public ResponseEntity<?> createRatio(@PathVariable String passport, @RequestParam String callback){
        return redirect(ratioService.authorize(passport, callback));
    }

    @RequestMapping(path = "/solvency/ratio/callback")
    public ResponseEntity<?> callback(@RequestParam(required = false) String code, @RequestParam(required = false) String error, @RequestParam(required = false) String state){
        if(error!=null){
            log.info("User cancel auth: {}", error);
            log.info("Redirect user to callback: {}", state);
            return redirect(state);
        }
        return redirect(ratioService.verify(code));
    }

    private static ResponseEntity<String> redirect(String uri){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", uri);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
