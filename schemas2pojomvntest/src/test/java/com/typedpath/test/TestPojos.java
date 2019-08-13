package com.typedpath.test;

import java.util.Arrays;

import com.typedpath.main.Address;
import com.typedpath.main.Person;
import com.typedpath.main.PersonId;

public class TestPojos {
public void testPojoCreation() {
    Person person = Person.person()
            .withAddress(Address.address()
               .withAddr1("addr1")
                    .withAddr2("addr2")
                    .withAddr3("addr3")
                    .withAddr4("addr4")
                    .withHousenumber(123)
                    .build())
            .withGolfHandicap(5)
            .withFavoriteColor(Person.PersonFavoriteColor.personFavoriteColor()
                    .withBlue(128)
                    .withColorId("deepBlue")
                    .build())
            .withPersonId(PersonId.personId()
                    .withNationalInsuranceNumber("NMXYZDFG")
                    .withPassportNumber("1234567")
                    .build())
            .withPreviousAddresses(Arrays.asList(Address.address().build()))
            .build();
}
}
