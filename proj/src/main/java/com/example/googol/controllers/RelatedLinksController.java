package com.example.googol.controllers;

import java.rmi.RemoteException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import search.IClientGateway;

@Controller
public class RelatedLinksController {
    @Autowired
    private IClientGateway gateway;

    @GetMapping("/related-links")
    public String getRelatedLinks(@RequestParam("link") String link, Model model) throws RemoteException {
        List<String> relatedLinks = gateway.request_url_related(link);
        model.addAttribute("link", link);
        model.addAttribute("relatedLinks", relatedLinks);
        return "related_links"; 
    }
    
}
