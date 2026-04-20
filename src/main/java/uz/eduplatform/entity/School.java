package uz.eduplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity @Table(name = "schools")
@Data @NoArgsConstructor
@ToString(exclude = "district")
public class School {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String number;
    @ManyToOne @JoinColumn(name = "district_id")
    @JsonIgnore
    private District district;
}