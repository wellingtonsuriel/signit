package zw.co.telone.signit.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zw.co.telone.signit.model.Document;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findBySignersContaining(String username);

    @Query("SELECT d FROM Document d WHERE d.createdby = :createdby AND d.allsigned = 1")
    List<Document> findSignedDocumentsByCreatedBy(@Param("createdby") String createdby);




}