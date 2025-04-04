package com.example.googol.controllers;
import java.rmi.RemoteException;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import java.util.List;
import search.*;


@Controller
public class HomeController {
    
    @GetMapping("/")
    public String home() {
        return "greeting";
    }
}