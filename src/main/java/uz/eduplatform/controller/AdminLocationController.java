package uz.eduplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/location")
@RequiredArgsConstructor
public class AdminLocationController {

    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final SchoolRepository schoolRepository;

    @PostMapping("/regions")
    public ResponseEntity<Region> addRegion(@RequestBody Map<String, String> body) {
        Region r = new Region();
        r.setName(body.get("name"));
        return ResponseEntity.ok(regionRepository.save(r));
    }

    @DeleteMapping("/regions/{id}")
    public ResponseEntity<Void> deleteRegion(@PathVariable Long id) {
        regionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/districts")
    public ResponseEntity<District> addDistrict(@RequestBody Map<String, Object> body) {
        District d = new District();
        d.setName((String) body.get("name"));
        d.setCity(Boolean.TRUE.equals(body.get("isCity")));
        d.setRegion(regionRepository.getReferenceById(Long.valueOf(body.get("regionId").toString())));
        return ResponseEntity.ok(districtRepository.save(d));
    }

    @DeleteMapping("/districts/{id}")
    public ResponseEntity<Void> deleteDistrict(@PathVariable Long id) {
        districtRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/schools")
    public ResponseEntity<School> addSchool(@RequestBody Map<String, Object> body) {
        School s = new School();
        s.setName((String) body.get("name"));
        s.setNumber((String) body.getOrDefault("number", ""));
        s.setDistrict(districtRepository.getReferenceById(Long.valueOf(body.get("districtId").toString())));
        return ResponseEntity.ok(schoolRepository.save(s));
    }

    @DeleteMapping("/schools/{id}")
    public ResponseEntity<Void> deleteSchool(@PathVariable Long id) {
        schoolRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/regions")
    public ResponseEntity<List<Region>> getAllRegions() {
        return ResponseEntity.ok(regionRepository.findAll());
    }

    @GetMapping("/districts/{regionId}")
    public ResponseEntity<List<District>> getDistricts(@PathVariable Long regionId) {
        return ResponseEntity.ok(districtRepository.findByRegionId(regionId));
    }

    @GetMapping("/schools/{districtId}")
    public ResponseEntity<List<School>> getSchools(@PathVariable Long districtId) {
        return ResponseEntity.ok(schoolRepository.findByDistrictId(districtId));
    }
}