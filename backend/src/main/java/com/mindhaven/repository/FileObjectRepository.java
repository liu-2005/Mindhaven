package com.mindhaven.repository;

import java.util.List;
import java.util.Optional;

import com.mindhaven.entity.FileObject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileObjectRepository extends JpaRepository<FileObject, Long> {

    Optional<FileObject> findByIdAndDeletedAtIsNull(Long id);
}
