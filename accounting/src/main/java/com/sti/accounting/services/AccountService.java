package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.repositories.IAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.util.Arrays;
import java.util.List;


/*
* NOTA: por ahora para simplificar no vamos a utilizar interfaces
*       pero los metodos deberan estar relacionados solo con las acciones de las cuentas
* ejemplo: crear cuenta, eliminar cuenta, actualizar cuenta, obtener la cuenta
*  para efectos de ejemplo voy a utilizar object pero se debe crear una clase que cumpla las necesidades
*
* */

@Service
public class AccountService {
    //utilice el logger para saber que esta haciendo el servicio en cada metodo que se llama
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    //Injectar la dependecia de repositorio
    private final IAccountRepository iAccountRepository;


    //se agrega al constructor de la clase.
    //si necesita una accion de saldos puede inyectar un repositorio de saldos o si realiza un service
    //ya que ve la necesidad de reutilizar la funcionalidad puede inyectar un service(es la mejor opcion)
    public AccountService(IAccountRepository iAccountRepository) {

        this.iAccountRepository = iAccountRepository;
    }

    // creamos los metodos por ejemplo un getAll

    public List<Object> getAll(){
        // este get All obtiene todas las cuentas usando los metods del repositorio
//        List<AccountEntity> all = iAccountRepository.findAll().stream().map(x->{
//            // aqui puede hacer la transformacion a un nuevo objeto
//        }).collect(Collectors.toSet());
        logger.info("getAll");
        return Arrays.asList(iAccountRepository.findAll().toArray());
    }

    //Ejemplo con un getById
    public Object getById(Long id){
        logger.trace("peticion de cuenta con id {} ", id);

        //vamos a validar si existe el id
        //lo que hace esta linea es que si no existe lanza una excepcion y no continua con el codigo
        //esto despues sera obtenido en una exception global
        AccountEntity accountEntity = iAccountRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No se encontro cuenta con id %s", id))
        );
        // aqui hace la transformacion a un nuevo objeto

        return accountEntity;
    }

    //el caso de un post seria similar
    public Object createAccount(Object object){
        //aqui podria comenzar las validaciones
        logger.info("creando cuenta");
        //lanzar una excepcion en caso que no cumpla algo
        logger.error("no se pudo crear la cuenta error en ...");
        //en caso de todo Ok puede guardar
        //debe crear el entity que recibira el metodo save
       // iAccountRepository.save(accountEntity);


        return null;
    }

}
