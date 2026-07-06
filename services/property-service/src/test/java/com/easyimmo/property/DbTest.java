package com.easyimmo.property;

import com.easyimmo.property.dto.OwnerRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

public class DbTest {

    @Test
    public void testOwnerRequestValidation() {
        System.out.println("=== START VALIDATION DIAGNOSTIC ===");
        
        OwnerRequest request = new OwnerRequest();
        request.setFirstName("SOMDA");
        request.setLastName("Modeste");
        request.setPhone("+22677882654");
        request.setEmail("modestesomda@gmail.com");
        request.setNationalId("BC93849");
        request.setSharePercentage(BigDecimal.valueOf(80));
        request.setAddress("Ouaga");
        request.setBankName("BF9438");
        request.setBankAccount("34934899");
        request.setMobileMoneyPhone("");
        request.setMobileMoneyProvider("");
        request.setNotes("");
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        
        Set<ConstraintViolation<OwnerRequest>> violations = validator.validate(request);
        
        System.out.println("Violations count: " + violations.size());
        for (ConstraintViolation<OwnerRequest> violation : violations) {
            System.out.println("Field: " + violation.getPropertyPath() + " | Message: " + violation.getMessage() + " | Value: '" + violation.getInvalidValue() + "'");
        }
        
        System.out.println("=== END VALIDATION DIAGNOSTIC ===");
    }
}
