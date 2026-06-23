const fs = require('fs');
const path = require('path');

const BASE_PACKAGE = 'com.sima.backend';
const BASE_DIR = 'src/main/java/com/sima/backend';

const fileStructure = {
  'controller': [
    'AuthController', 'UsuarioController', 'AdultoMayorController',
    'DispositivoIotController', 'MedicamentoController', 'RegistroTomaController',
    'AlertaController', 'ConfiguracionController'
  ],
  'service': [
    'UsuarioService', 'UsuarioServiceImpl', 'AdultoMayorService', 'AdultoMayorServiceImpl',
    'AuthService', 'AuthServiceImpl', 'MedicamentoService', 'MedicamentoServiceImpl',
    'DispositivoIotService', 'DispositivoIotServiceImpl', 'AlertaService', 'AlertaServiceImpl',
    'RegistroTomaService', 'RegistroTomaServiceImpl'
  ],
  'entity': [
    'Rol', 'Usuario', 'AdultoMayor', 'RelacionUsuarioAdulto', 'DispositivoIot',
    'Medicamento', 'HorarioMedicamento', 'RegistroToma', 'EventoIot', 'Alerta',
    'NotificacionWechat', 'ObservacionCuidador', 'ConfiguracionSistema', 'AuditoriaLog'
  ],
  'dto/request': [
    'LoginRequest', 'UsuarioCreateRequest', 'AdultoMayorRequest', 'MedicamentoRequest',
    'RegistroTomaRequest', 'DispositivoIotRequest'
  ],
  'dto/response': [
    'LoginResponse', 'UsuarioResponse', 'AdultoMayorResponse', 'MedicamentoResponse',
    'AlertaResponse', 'ApiResponse'
  ],
  'repository': [
    'RolRepository', 'UsuarioRepository', 'AdultoMayorRepository', 'RelacionUsuarioAdultoRepository',
    'DispositivoIotRepository', 'MedicamentoRepository', 'HorarioMedicamentoRepository',
    'RegistroTomaRepository', 'EventoIotRepository', 'AlertaRepository', 'NotificacionWechatRepository',
    'ObservacionCuidadorRepository', 'ConfiguracionSistemaRepository', 'AuditoriaLogRepository'
  ],
  'exception': [
    'AppException', 'ResourceNotFoundException', 'UnauthorizedException', 'BadRequestException',
    'GlobalExceptionHandler'
  ],
  'util': [
    'MapperUtil', 'ValidationUtil', 'DateUtil'
  ]
};

for (const [folder, files] of Object.entries(fileStructure)) {
  const dirPath = path.join(BASE_DIR, folder);
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }

  for (const fileName of files) {
    const filePath = path.join(dirPath, `${fileName}.java`);
    
    // Solo creamos si no existe (por si acaso el usuario ya editó algo)
    if (!fs.existsSync(filePath)) {
      const packagePath = folder.replace(/\//g, '.');
      let classType = 'class';
      let content = '';

      // Interfaces para Services
      if (folder === 'service' && !fileName.endsWith('Impl')) {
        classType = 'interface';
      }
      
      // Interfaces para Repositories
      if (folder === 'repository') {
        classType = 'interface';
        content = `package ${BASE_PACKAGE}.${packagePath};\n\nimport org.springframework.data.jpa.repository.JpaRepository;\nimport org.springframework.stereotype.Repository;\nimport ${BASE_PACKAGE}.entity.${fileName.replace('Repository', '')};\n\n@Repository\npublic interface ${fileName} extends JpaRepository<${fileName.replace('Repository', '')}, Long> {\n}\n`;
      } 
      // Controladores
      else if (folder === 'controller') {
        content = `package ${BASE_PACKAGE}.${packagePath};\n\nimport org.springframework.web.bind.annotation.RestController;\nimport org.springframework.web.bind.annotation.RequestMapping;\n\n@RestController\n@RequestMapping("/api/${fileName.replace('Controller', '').toLowerCase()}s")\npublic class ${fileName} {\n}\n`;
      }
      // Servicios Impl
      else if (folder === 'service' && fileName.endsWith('Impl')) {
        const interfaceName = fileName.replace('Impl', '');
        content = `package ${BASE_PACKAGE}.${packagePath};\n\nimport org.springframework.stereotype.Service;\n\n@Service\npublic class ${fileName} implements ${interfaceName} {\n}\n`;
      }
      // Exception Handler
      else if (fileName === 'GlobalExceptionHandler') {
        content = `package ${BASE_PACKAGE}.${packagePath};\n\nimport org.springframework.web.bind.annotation.ControllerAdvice;\n\n@ControllerAdvice\npublic class ${fileName} {\n}\n`;
      }
      // Exceptions
      else if (folder === 'exception') {
        content = `package ${BASE_PACKAGE}.${packagePath};\n\npublic class ${fileName} extends RuntimeException {\n    public ${fileName}(String message) {\n        super(message);\n    }\n}\n`;
      }
      // Genericos (Entities, DTOs, Utils)
      else {
        content = `package ${BASE_PACKAGE}.${packagePath};\n\npublic ${classType} ${fileName} {\n}\n`;
      }

      fs.writeFileSync(filePath, content, 'utf8');
      console.log(`Created: ${filePath}`);
    }
  }
}

console.log('All missing files generated successfully.');
