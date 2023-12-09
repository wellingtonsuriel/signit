package zw.co.telone.signit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import zw.co.telone.signit.model.Signature;

import java.util.List;

public interface SignatureRepository extends JpaRepository<Signature, Long> {
    @Query("SELECT s FROM Signature s WHERE s.username = :username")
    Signature findByUsername(String username);
}
