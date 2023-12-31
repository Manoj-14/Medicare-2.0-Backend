package com.project.medicare.service;

import com.project.medicare.config.ModelMapperConfig;
import com.project.medicare.dto.UserDto;
import com.project.medicare.entity.*;
import com.project.medicare.exception.MedicineInActiveException;
import com.project.medicare.exception.MedicineNotFoundException;
import com.project.medicare.exception.UserNotFoundException;
import com.project.medicare.jwt.service.ApplicationUserDetailsService;
import com.project.medicare.mapper.PurchaseMapper;
import com.project.medicare.repository.CartRepository;
import com.project.medicare.repository.MedicineRepository;
import com.project.medicare.repository.PurchaseRepository;
import com.project.medicare.repository.UserRepository;
import com.project.medicare.utils.Log;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService{

    @Autowired
    UserRepository userRepo;
    @Autowired
    MedicineRepository medicineRepository;
    @Autowired
    MedicineService medicineService;
    @Autowired
    PurchaseRepository purchaseRepository;
    @Autowired
    CartRepository cartRepository;
    @Autowired
    ApplicationUserDetailsService userDetailService;

    private final ModelMapper mapper;

    private UserDto entityToDto(User user){
        return mapper.map(user,UserDto.class);
    }

    private User dtoToEntity(UserDto userDto){
        return mapper.map(userDto,User.class);
    }

    @Override
    public List<User> findAll() {
        return userRepo.findAll();
    }

    @Override
    public UserDto create(UserDto user) throws DuplicateKeyException, NoSuchAlgorithmException {
        if(userRepo.existsByEmail(user.getEmail())){
            throw new DuplicateKeyException("User Already exists");
        }
        else{
            byte[] salt = createSalt();
            byte[] hashedPassword = createPasswordHash(user.getPassword(),salt);
            User userEntity = dtoToEntity(user);
            userEntity.setStoredSalt(salt);
            userEntity.setPassword(hashedPassword);
            userRepo.save(userEntity);
            return user;
        }
    }

    @Override
    public User findUser(int id) throws UserNotFoundException {
        User user = userRepo.findById(id);
        if(user != null)  return user;
        else throw new UserNotFoundException();
    }

    @Override
    public User findUser(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email);
        if(user != null)  return user;
        else throw new UsernameNotFoundException("Email Not found");
    }

    private byte[] createSalt() {
        var random = new SecureRandom();
        var salt = new byte[128];
        random.nextBytes(salt);
        return salt;
    }

    private byte[] createPasswordHash(String password, byte[] salt) throws NoSuchAlgorithmException {
        var md = MessageDigest.getInstance("SHA-512");
        md.update(salt);
        return md.digest(password.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public User authenticate(String email, String password) throws UserNotFoundException {
//        User user = userRepo.findUserByEmailAndPassword(email, password);
        User user = new User();
        if(user == null) throw new UserNotFoundException();
        else {
            return user;
        }
    }

    @Override
    public void changePassword(String email, String old_password, String new_password) throws UserNotFoundException, VerifyError, NoSuchAlgorithmException {
        User user = this.findUser(email);
        if(user == null) throw new UserNotFoundException();
        else{
            if (!userDetailService.verifyPasswordHash(old_password,user.getPassword(),user.getStoredSalt())){
                throw new VerifyError();
            }
            else{
                byte[] salt = createSalt();
                byte[] hashedPassword = createPasswordHash(new_password,salt);
                user.setStoredSalt(salt);
                user.setPassword(hashedPassword);
                userRepo.save(user);
            }
        }
    }

    @Transactional
    @Override
    public void addToCart(String userEmail,int medicineId) throws MedicineNotFoundException, MedicineInActiveException, UserNotFoundException {
        try{
            Log.DEBUG("Medicine ID : "+medicineId);
            Log.DEBUG("User Email : "+ userEmail);
            Medicine dbMedicine = medicineService.getMedicine(medicineId);
            if(dbMedicine.isActive()){
                User user = this.findUser(userEmail);
                if(userRepo.existsByEmailAndCart_Medicines_Id(userEmail,medicineId)){
                    Cart userCart = userRepo.findCartByEmailAndMedicineId(userEmail,medicineId);
                    userCart.setQuantity(userCart.getQuantity() +1);
                    userCart.setAmount(userCart.getQuantity() * userCart.getMedicines().getPrice());
                }
                else {
                    Cart newCart = new Cart(dbMedicine , 1,dbMedicine.getPrice() );
                    Log.INFO(this,newCart.toString());
                    user.getCart().add(newCart);
                }
                userRepo.save(user);
            }
            else {
                throw new MedicineInActiveException("Medicine is Inactive");
            }
        }catch (MedicineNotFoundException ene){
            throw new MedicineNotFoundException();
        }
    }

    @Override
    public void removeFromCart(String userEmail, int medicineId) throws UserNotFoundException, MedicineNotFoundException {
        try{
            Medicine dbMedicine = medicineService.getMedicine(medicineId);

                User user = this.findUser(userEmail);
                if(userRepo.existsByEmailAndCart_Medicines_Id(userEmail,medicineId)){
                    Cart userCart = user.getCart().stream()
                            .filter(cartItem -> cartItem.getMedicines().getId() == medicineId)
                            .findFirst()
                            .orElse(null);
                    if(userCart.getQuantity() == 1){
                        user.getCart().remove(userCart);
                        cartRepository.delete(cartRepository.findById(userCart.getId()));
                    }
                    else {
                        userCart.setQuantity(userCart.getQuantity()-1);
                        userCart.setAmount(userCart.getQuantity() * userCart.getMedicines().getPrice());
                    }
                }
                else {
                    throw new EntityNotFoundException("Medicine not found");
                }
                userRepo.save(user);

        } catch (MedicineNotFoundException e) {
            throw new MedicineNotFoundException();
        }
    }

    @Transactional
    @Override
    public void purchaseMedicines(String email, int medicineId, int quantity,int totalAmount) throws  MedicineInActiveException,UserNotFoundException {
        try{
            Medicine dbMedicine = medicineService.getMedicine(medicineId);
            User user = this.findUser(email);
            Log.INFO(this,"DB Medicine:"+dbMedicine.toString());
            if(dbMedicine.isActive()){
                Purchase newPurchase = new Purchase(dbMedicine,quantity,totalAmount);
                purchaseRepository.save(newPurchase);
                Log.INFO(this,purchaseRepository.findById(newPurchase.getId()).toString());
                user.getPurchases().add(newPurchase);
            }
            else{
                throw new MedicineInActiveException("Medicine is in active");
            }
            userRepo.save(user);
        }catch (MedicineInActiveException mie){
            throw new MedicineInActiveException("Medicine is inactive");
        } catch (MedicineNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void purchaseMedicines(String email, List<PurchaseMapper> purchases) throws MedicineInActiveException, UserNotFoundException, MedicineNotFoundException {
        try{
            User user = this.findUser(email);
            for(PurchaseMapper purchaseMap: purchases){
                Medicine medicine = medicineService.getMedicine(purchaseMap.getMedicineId());
                Purchase purchase = new Purchase(medicine,purchaseMap.getQuantity(),purchaseMap.getTotalAmount());
                user.getPurchases().add(purchase);
                this.removeMedicineFromCart(email,medicine.getId());
            }
            userRepo.save(user);
        }catch (MedicineInActiveException mie){
            throw new MedicineInActiveException("Medicine is inactive");
        } catch (MedicineNotFoundException e) {
            throw new MedicineNotFoundException();
        }
    }

    @Override
    public List<Cart> getUserCart(String email) throws UserNotFoundException, NoSuchAlgorithmException {
        if(userRepo.existsByEmail(email)) {
            return this.findUser(email).getCart();
        }else{
            throw new UserNotFoundException();
        }
    }

    @Override
    public void removeMedicineFromCart(String userEmail, int medicineId) throws MedicineNotFoundException, UserNotFoundException {
        try{
            Medicine dbMedicine = this.medicineService.getMedicine(medicineId);
            User user = this.findUser(userEmail);
            if(userRepo.existsByEmailAndCart_Medicines_Id(userEmail,medicineId)){
                Cart userCart = user.getCart().stream()
                        .filter(cartItem -> cartItem.getMedicines().getId() == medicineId)
                        .findFirst()
                        .orElse(null);
                user.getCart().remove(userCart);
                cartRepository.delete(cartRepository.findById(userCart.getId()));
            }
            else {
                throw new EntityNotFoundException("Medicine not found");
            }
            userRepo.save(user);
        }catch (MedicineNotFoundException mne){
            throw new MedicineNotFoundException();
        }
    }

        public UserDto getProfile(String email) throws UserNotFoundException{
        if(userRepo.existsByEmail(email)){
            User user = this.findUser(email);
            return this.entityToDto(user);
        } else{
            throw new UserNotFoundException();
        }

    }

    @Override
    public void updateAddress(String email, Address address) throws UserNotFoundException {
        User user = this.findUser(email);
        if(userRepo.existsByEmail(email)){
            user.setAddress(address);
        }else{
            throw new UserNotFoundException();
        }
        userRepo.save(user);
    }

    @Override
    public void updatePhone(String email, long phone) throws UserNotFoundException {
        User user = this.findUser(email);
        if(userRepo.existsByEmail(email)){
            user.setPhone(phone);
        }else{
            throw new UserNotFoundException();
        }
        userRepo.save(user);
    }

    @Override
    public List<Purchase> getUserPurchases(String email) throws UserNotFoundException, NoSuchAlgorithmException {
        if(userRepo.existsByEmail(email)) {
            return this.findUser(email).getPurchases();
        }else {
            throw new UserNotFoundException();
        }
    }
}
