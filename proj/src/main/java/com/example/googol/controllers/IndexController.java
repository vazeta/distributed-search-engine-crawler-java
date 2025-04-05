package com.example.googol.controllers;

import java.rmi.RemoteException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import search.IClientGateway;

@Controller
public class IndexController {
    @Autowired
    private IClientGateway gateway;

    @PostMapping("/add-url")
    public String addUrl(@RequestParam("url") String url, Model model) throws RemoteException {
        try {
            gateway.addUrlToQueue(url);
            model.addAttribute("message", "O link está a ser processado!");
        } catch (Exception e) {
            model.addAttribute("error", "O indexamento do link falhou.");
        }
        return "greeting";  // retorna para a mesma página de index
    }
}

