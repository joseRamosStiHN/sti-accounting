package com.sti.accounting.controllers;


import com.sti.accounting.services.AccountService;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/v1/accounting")
public class AccountingController {

    //inyectamos el servicio
    private final AccountService accountService;

    public AccountingController(AccountService accountService) {
        this.accountService = accountService;
    }


    @GetMapping()
    public List<Object> getAccounts(){
        // el utilizarlos es facil solo debemos llamarlos segun la firma que tenga
        return accountService.getAll();
    }

    @GetMapping("/{id}")
    public Object getAccountById(@PathVariable("id") Long id){
        return accountService.getById(id);
    }

    @PostMapping()
    public Object createAccount(@RequestBody Object object){
        return accountService.createAccount(object);
    }


    @PutMapping("/{id}")
    public Object updateAccount(@PathVariable("id") Long id, @RequestBody Object object){
        return null;
    }

    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable("id") Long id){

    }

}
