package com.project.medicare.service;

import com.project.medicare.aws.AwsBucketService;
import com.project.medicare.entity.Medicine;
import com.project.medicare.exception.EntityCreatingException;
import com.project.medicare.exception.MedicineNotFoundException;
import com.project.medicare.repository.MedicineRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class MedicineServiceImpl implements MedicineService {

    @Autowired
    MedicineRepository medicineRepository;

    @Autowired
    AwsBucketService awsService;

    @Override
    @Transactional
    public Medicine add(Medicine medicine, MultipartFile image) throws EntityCreatingException {
        try {
            medicineRepository.save(medicine);
            medicine.setImage(awsService.uploadFile(image,medicine.getId()+""));
            medicineRepository.save(medicine);
            return medicine;
        }catch (Exception ec){
            throw new EntityCreatingException();
        }
    }
    @Override
    @Transactional
    public void remove(int id) throws EntityNotFoundException{
        Medicine medicine = medicineRepository.findById(id);
        if(medicine != null){
            medicineRepository.deleteById(id);
            System.out.println(awsService.deleteFileFromBucket(id+""));
        }
        else {
            throw new EntityNotFoundException();
        }
    }
    @Override
    @Transactional
    public void enableOrDisable(int id) throws EntityNotFoundException{
        Medicine medicine = medicineRepository.findById(id);
        if(medicine != null){
            medicine.setActive(!medicine.isActive());
            medicineRepository.save(medicine);
        }else{
            throw new EntityNotFoundException();
        }
    }

    @Override
    @Transactional
    public void update(Medicine medicine) throws EntityCreatingException,MedicineNotFoundException{
        try{
            if(medicineRepository.existsById(medicine.getId())){
                Medicine dbMedicine = medicineRepository.findById(medicine.getId());
                dbMedicine.setName(medicine.getName());
                dbMedicine.setDescription(medicine.getDescription());
                dbMedicine.setQuantity(medicine.getQuantity());
                dbMedicine.setSeller(medicine.getSeller());
                dbMedicine.setPrice(medicine.getPrice());
                medicineRepository.save(dbMedicine);
            }
            else{
                throw new MedicineNotFoundException();
            }
        }catch (Exception ec){
            throw new EntityCreatingException("Error in updating medicine");
        }
    }

    @Override
    public Medicine getMedicine(int id) throws MedicineNotFoundException {
        Medicine medicine = medicineRepository.findById(id);
        if(medicine == null){
            throw new MedicineNotFoundException();
        }
        else{
            return medicine;
        }
    }

    @Override
    public List<Medicine> getMedicines() {
        return (List<Medicine>) medicineRepository.findAll();
    }


}
