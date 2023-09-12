package com.adamgent.petclinic.repo;

import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;

@Entity
public record Vet(@GeneratedValue(strategy = GenerationType.IDENTITY) @Id long id, //
		String firstName, //
		String lastName) implements Person {

}
