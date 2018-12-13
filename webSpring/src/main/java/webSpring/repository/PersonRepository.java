package webSpring.repository;

import org.springframework.data.repository.CrudRepository;

import webSpring.entity.Person;

// This will be AUTO IMPLEMENTED by Spring into a Bean called studentRepository
// CRUD refers Create, Read, Update, Delete

public interface PersonRepository extends CrudRepository<Person, Integer> {

}
