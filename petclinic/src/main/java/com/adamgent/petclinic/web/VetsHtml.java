package com.adamgent.petclinic.web;

import java.util.List;

import com.adamgent.petclinic.repo.Vet;

import io.jstach.jstache.JStache;

@JStache(path = "vets/vetList")
public record VetsHtml(List<Vet> vets) {

}