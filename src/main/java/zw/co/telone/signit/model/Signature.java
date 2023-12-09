package zw.co.telone.signit.model;



import jakarta.persistence.*;

import lombok.*;


import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String username;



    private String path;





}