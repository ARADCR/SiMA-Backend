package com.sima.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * Guarda un archivo en el almacenamiento local
     * @param file Archivo a guardar
     * @param folder Carpeta de destino (ej. "credenciales")
     * @return Nombre final del archivo guardado
     */
    String storeFile(MultipartFile file, String folder);
}
