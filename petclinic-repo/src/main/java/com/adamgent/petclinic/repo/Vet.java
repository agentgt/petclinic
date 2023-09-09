package com.adamgent.petclinic.repo;

import org.seasar.doma.Entity;

@Entity
public record Vet(long id, String firstName, String lastName) {

}
