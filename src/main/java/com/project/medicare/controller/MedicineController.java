package com.project.medicare.controller;

import com.project.medicare.aws.AwsBucketService;
import com.project.medicare.entity.Medicine;
import com.project.medicare.exception.EntityCreatingException;
import com.project.medicare.exception.MedicineNotFoundException;
import com.project.medicare.service.MedicineService;
import com.project.medicare.utils.Log;
import jakarta.persistence.EntityNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    @Autowired
    MedicineService medicineService;

    @Autowired
    private AwsBucketService awsService;

    @GetMapping
    public ResponseEntity<?> getAll(){
        try{
            List<Medicine> medicines = medicineService.getMedicines();
            return new ResponseEntity<>(medicines,HttpStatus.OK);
        }catch (Exception ex){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getById(@PathVariable int id){
        try{
            Medicine medicine = medicineService.getMedicine(id);
            return new ResponseEntity<Medicine>(medicine,HttpStatus.OK);
        }catch (MedicineNotFoundException ene){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Medicine not found");
        }
    }

    @PostMapping(value = "/create",consumes = "multipart/form-data")
    public ResponseEntity<?> create(@RequestParam String name,@RequestParam String description, @RequestParam int quantity,@RequestParam double price,@RequestParam String seller,@RequestParam MultipartFile image){
        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setDescription(description);
        medicine.setPrice(price);
        medicine.setSeller(seller);
        medicine.setQuantity(quantity);
        try{
            Medicine dbMedicine = medicineService.add(medicine,image);
            return new ResponseEntity<>(dbMedicine,HttpStatus.OK);
        } catch (EntityCreatingException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Unable to create medicine");
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@NotNull @RequestBody Medicine medicine){
        try{
            medicineService.update(medicine);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EntityCreatingException e) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);
        } catch (MedicineNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Medicine not found");
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable int id){
        try{
            medicineService.remove(id);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (EntityNotFoundException ene){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Medicine not found");
        }
    }

    @PutMapping("/isenabled")
    public ResponseEntity<?> enableOrDisabled(@RequestParam int enable){
        try{
            medicineService.enableOrDisable(enable);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (EntityNotFoundException ene){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

}
