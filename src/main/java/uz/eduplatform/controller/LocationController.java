package uz.eduplatform.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;

import java.util.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    @GetMapping("/regions")
    public ResponseEntity<List<Region>> getRegions() {
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

    @PostMapping("/select-school")
    public ResponseEntity<Map<String, Object>> selectSchool(
            @RequestBody Map<String, Long> body,
            Authentication auth) {

        User user = (User) auth.getPrincipal();
        user = userRepository.findById(user.getId()).orElse(user);

        if (!user.canChangeSchool()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Maktabni 3 martadan ko'p o'zgartirib bo'lmaydi!",
                    "changeCount", user.getSchoolChangeCount()
            ));
        }

        Long schoolId = body.get("schoolId");
        School school = schoolRepository.findById(schoolId).orElse(null);
        if (school == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Maktab topilmadi"));
        }

        if (user.getSchool() != null) {
            user.setSchoolChangeCount(user.getSchoolChangeCount() + 1);
        }
        user.setSchool(school);
        userRepository.save(user);

        int remaining = 3 - user.getSchoolChangeCount();
        return ResponseEntity.ok(Map.of(
                "message", "Maktab muvaffaqiyatli saqlandi",
                "school", school.getName(),
                "changesRemaining", remaining
        ));
    }

    @GetMapping("/my-school")
    public ResponseEntity<Map<String, Object>> getMySchool(Authentication auth) {
        User user = (User) auth.getPrincipal();
        user = userRepository.findById(user.getId()).orElse(user);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("hasSchool", user.getSchool() != null);
        res.put("changesRemaining", 3 - user.getSchoolChangeCount());

        if (user.getSchool() != null) {
            School s = user.getSchool();
            District d = s.getDistrict();
            Region r = d != null ? d.getRegion() : null;

            res.put("schoolId", s.getId());
            res.put("schoolName", s.getName());
            res.put("schoolNumber", s.getNumber());
            res.put("districtName", d != null ? d.getName() : "-");
            res.put("regionName", r != null ? r.getName() : "-");
        }
        return ResponseEntity.ok(res);
    }
}