package uz.eduplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.util.List;

@Entity @Table(name = "districts")
@Data @NoArgsConstructor
@ToString(exclude = {"region", "schools"})
public class District {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private boolean isCity = false;
    @ManyToOne @JoinColumn(name = "region_id")
    @JsonIgnore
    private Region region;
    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<School> schools;
}