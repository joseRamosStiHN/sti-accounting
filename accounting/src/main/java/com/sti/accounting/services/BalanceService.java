package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.*;
import com.sti.accounting.utils.AppUtil;
import com.sti.accounting.utils.Motion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Service
public class BalanceService {

    private static final Logger logger = LoggerFactory.getLogger(BalanceService.class);

    private final ITransactionBalanceGeneralRepository view;

    public BalanceService( ITransactionBalanceGeneralRepository view) {
        this.view = view;
    }

    private BalanceGeneralResponse toDto(TransactionBalanceGeneralEntity entity){
        BalanceGeneralResponse dto = new BalanceGeneralResponse();
        dto.setId(entity.getId());
        dto.setCategory(entity.getCategory());
        dto.setAmount(entity.getCredit().subtract(entity.getDebit()));
        dto.setRoot(entity.isRoot());
        dto.setAccountName(entity.getAccountName());
        dto.setParentId(entity.getParentId());
        return dto;
    }

    //@Transactional <- remover Transactional para evitar bloquear la tabla
    public List<BalanceGeneralResponse> getBalanceGeneral() {
        //TransactionBalanceGeneralEntity collect aquí puedes usar la keyword var si se te hace muy largo el nombre o como curiosidad
        // var es para decile al compilador que lo va a inferir en tiempo de compilation
        var entities = StreamSupport.stream(view.findAll().spliterator(), false).toList();
        //(la otra opción es en ITransactionBalanceGeneralRepository CrudRepository por ListCrudRepository esto retorna una lista por defecto en el findAll())

        //roots first level: sacamos lo padres con lambdas
        List<BalanceGeneralResponse> roots = entities.stream().filter(TransactionBalanceGeneralEntity::isRoot).map(this::toDto).toList();
        //others (root = true && parentId != null) second level: sacamos los hijos con lambdas
        List<BalanceGeneralResponse> otherLevels = entities.stream().filter(x -> !x.isRoot() && x.getParentId() != null).map(this::toDto).toList();
        //map children, se mapean los hijos con los padres, ojo aqui estoy con los que no son roots pero pueden ser parent
        Map<Integer, BalanceGeneralResponse> parents = new HashMap<>();
        for (BalanceGeneralResponse other : otherLevels) {
            parents.put(other.getId(), other);
        }
        //aqui hago la suma de los hijos con los padres o sea agregar los valores a los padres siempre de los que no son root
        for (BalanceGeneralResponse item: otherLevels){
            Integer parentId = item.getParentId();
            if (parents.containsKey(parentId)){
                BalanceGeneralResponse current = parents.get(parentId);
                BigDecimal amount = current.getAmount().add(item.getAmount());
                current.setAmount(amount);
            }
        }
        //creando una lista con todos los padres y los hijos
        List<BalanceGeneralResponse> All = new ArrayList<>(roots);
        List<BalanceGeneralResponse> values = parents.values().stream().toList();
        All.addAll(values);

        // RootMaps
        //agregando los hijos(que pueden ser padres) a los padres
        Map<Integer, BalanceGeneralResponse> rootMaps = new HashMap<>();
        for (BalanceGeneralResponse root : All) {
            rootMaps.put(root.getId(), root);
        }
        // sumando los valores a los padres
        for (BalanceGeneralResponse root : All) {
            if (root.getParentId() != null) {
                BalanceGeneralResponse parent = rootMaps.get(root.getParentId());
                BigDecimal amount = parent.getAmount().add(root.getAmount());
                parent.setAmount(amount);
            }
        }
        //aquí lo que se hace es la transformación del Map<Integer, BalanceGeneralResponse> a lista
        // set parents
        return rootMaps.values().stream().toList();



//       try {
//           List<TransactionBalanceGeneralEntity> response = new ArrayList<>();
//           Iterable<TransactionBalanceGeneralEntity> data = view.findAll();
//           data.forEach(response::add);
//           Map<Integer, BalanceGeneralResponse> resultProcess = AppUtil.processTree(response);
//           List<BalanceGeneralResponse> result = new ArrayList<>();
//
//           Iterator<Map.Entry<Integer, BalanceGeneralResponse>> convert = resultProcess.entrySet().iterator();
//
//           while (convert.hasNext()) {
//               Map.Entry<Integer, BalanceGeneralResponse> object = convert.next();
//               result.add(object.getValue());
//           }
//           Map<Integer, BalanceGeneralResponse> treeRoot = AppUtil.buildBalanceGeneralTree(AppUtil.buildBalanceGeneral(response), false);
//           Iterator<Map.Entry<Integer, BalanceGeneralResponse>> sonSort = processResultRoot(treeRoot, resultProcess).entrySet().iterator();
//           while (sonSort.hasNext()) {
//               Map.Entry<Integer, BalanceGeneralResponse> object = sonSort.next();
//               result.add(object.getValue());
//           }
//           return result;
//       }catch (Exception e) {
//               throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Data");
//       }
    }


    private Map<Integer, BalanceGeneralResponse> processResultRoot(Map<Integer, BalanceGeneralResponse> tree, Map<Integer, BalanceGeneralResponse> sonRow) {
        Iterator<Map.Entry<Integer, BalanceGeneralResponse>> sortRoot = tree.entrySet().iterator();
        while (sortRoot.hasNext()) {
            Map.Entry<Integer, BalanceGeneralResponse> object = sortRoot.next();
            List<TransactionBalanceGeneralEntity> son = view.findByparentId(object.getKey());
            BigDecimal sumData = BigDecimal.ZERO;
            if (!son.isEmpty()) {
                for (TransactionBalanceGeneralEntity accountEntity : son) {
                    sumData = sumData.add(sonRow.get(accountEntity.getId()).getAmount());
                }
                BalanceGeneralResponse overWriteObject = object.getValue();
                overWriteObject.setAmount(overWriteObject.getAmount().add(sumData));
                tree.put(object.getKey(), overWriteObject);
            }
        }
        return tree;
    }

}
