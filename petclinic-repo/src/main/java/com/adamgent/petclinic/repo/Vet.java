package com.adamgent.petclinic.repo;

import org.seasar.doma.Entity;
import org.seasar.doma.jdbc.entity.NamingType;

@Entity(naming = NamingType.SNAKE_LOWER_CASE)
public record Vet(long id, String firstName, String lastName) {

}
