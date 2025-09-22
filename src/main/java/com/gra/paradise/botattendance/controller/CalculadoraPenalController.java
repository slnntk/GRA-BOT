package com.gra.paradise.botattendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller para a interface web da Calculadora Penal
 * 
 * @author Tiago Holanda
 */
@Controller
@RequestMapping("/")
public class CalculadoraPenalController {

    /**
     * Página principal da calculadora penal
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "Calculadora Penal - G.R.A Sistema");
        return "calculadora-penal";
    }

    /**
     * Página da calculadora penal (rota alternativa)
     */
    @GetMapping("/calculadora")
    public String calculadora(Model model) {
        model.addAttribute("title", "Calculadora Penal - G.R.A Sistema");
        return "calculadora-penal";
    }

    /**
     * Página da calculadora penal (rota alternativa)
     */
    @GetMapping("/calculadora-penal")
    public String calculadoraPenal(Model model) {
        model.addAttribute("title", "Calculadora Penal - G.R.A Sistema");
        return "calculadora-penal";
    }
}