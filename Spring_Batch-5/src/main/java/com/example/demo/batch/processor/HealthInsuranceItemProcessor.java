package com.example.demo.batch.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.demo.batch.dto.HealthInsuranceDTO;
import com.example.demo.batch.entity.HealthInsurance;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class HealthInsuranceItemProcessor implements ItemProcessor<HealthInsuranceDTO, HealthInsurance> {
    @Override
    public HealthInsurance process(final HealthInsuranceDTO item) throws Exception {
        final String fileName = item.fileName();
        final String payType = item.payType();
        final String nationalNo = item.nationalNo();
        final String cifNo = item.cifNo();
        final String withholdDate = item.withholdDate();
        final String withhold = item.withhold();
        final String reasonType = item.reasonType();

        final HealthInsurance healthInsurance = new HealthInsurance(fileName, payType, nationalNo, cifNo, withholdDate, withhold, reasonType);

        return healthInsurance;
    }
}
