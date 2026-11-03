package com.analisis.backend.repository;

import com.analisis.backend.model.entity.Place;
import com.analisis.backend.model.enums.PlaceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, String> {

    List<Place> findByCategory(PlaceCategory category);

    List<Place> findByMunicipalityIgnoreCase(String municipality);

    List<Place> findByCategoryAndMunicipalityIgnoreCase(PlaceCategory category, String municipality);

    @Query("SELECT DISTINCT p.municipality FROM Place p ORDER BY p.municipality ASC")
    List<String> findDistinctMunicipalities();
}
