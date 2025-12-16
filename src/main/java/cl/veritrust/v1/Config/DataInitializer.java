package cl.veritrust.v1.Config;

import cl.veritrust.v1.Model.Compra;
import cl.veritrust.v1.Model.Servicio;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Repository.CompraRepository;
import cl.veritrust.v1.Repository.ServicioRepository;
import cl.veritrust.v1.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Solo insertar datos si la base de datos está vacía
        if (usuarioRepository.count() == 0) {
            insertarUsuarios();
        }
        
        if (servicioRepository.count() == 0) {
            insertarServicios();
        }
        
        if (compraRepository.count() == 0 && usuarioRepository.count() > 0 && servicioRepository.count() > 0) {
            insertarCompras();
        }
    }

    private void insertarUsuarios() {
        Usuario admin = new Usuario();
        admin.setRut("218007244");
        admin.setNombre("Administrador Sistema");
        admin.setTelefono("+56912345678");
        admin.setEmail("admin@veritrust.cl");
        admin.setFechaNac(LocalDate.of(1990, 1, 15));
        admin.setContraseña(passwordEncoder.encode("admin123"));
        admin.setRol("admin");
        admin.setRegion("Región Metropolitana");
        admin.setComuna("Santiago");
        usuarioRepository.save(admin);

        Usuario cliente1 = new Usuario();
        cliente1.setRut("125732143");
        cliente1.setNombre("Juan Pérez");
        cliente1.setTelefono("+56987654321");
        cliente1.setEmail("juan.perez@email.com");
        cliente1.setFechaNac(LocalDate.of(1985, 5, 20));
        cliente1.setContraseña(passwordEncoder.encode("dilan123"));
        cliente1.setRol("user");
        cliente1.setRegion("Región de Valparaíso");
        cliente1.setComuna("Valparaíso");
        usuarioRepository.save(cliente1);

        Usuario cliente2 = new Usuario();
        cliente2.setRut("22222222-2");
        cliente2.setNombre("María González");
        cliente2.setTelefono("+56976543210");
        cliente2.setEmail("maria.gonzalez@email.com");
        cliente2.setFechaNac(LocalDate.of(1992, 8, 10));
        cliente2.setContraseña(passwordEncoder.encode("cliente123"));
        cliente2.setRol("user");
        cliente2.setRegion("Región Metropolitana");
        cliente2.setComuna("Las Condes");
        usuarioRepository.save(cliente2);

        Usuario cliente3 = new Usuario();
        cliente3.setRut("33333333-3");
        cliente3.setNombre("Carlos Rodríguez");
        cliente3.setTelefono("+56965432109");
        cliente3.setEmail("carlos.rodriguez@email.com");
        cliente3.setFechaNac(LocalDate.of(1988, 12, 3));
        cliente3.setContraseña(passwordEncoder.encode("cliente123"));
        cliente3.setRol("user");
        cliente3.setRegion("Región del Biobío");
        cliente3.setComuna("Concepción");
        usuarioRepository.save(cliente3);

        System.out.println("✅ Usuarios de prueba insertados correctamente");
    }

    private void insertarServicios() {
        Servicio servicio1 = new Servicio();
        servicio1.setNombre("Firma Digital Simple");
        servicio1.setDescripcion("Firma digital básica para documentos personales");
        servicio1.setDescripcionCompleta("<h2>Firma Digital Simple</h2><p>Servicio de firma digital básica ideal para documentos personales como contratos, acuerdos y documentos legales simples.</p><p><strong>Incluye:</strong></p><ul><li>Firma digital con validez legal</li><li>Certificado de autenticidad</li><li>Almacenamiento seguro por 1 año</li></ul>");
        servicio1.setPrecio(15000);
        servicio1.setDetalles(Arrays.asList(
            "Firma digital con validez legal",
            "Certificado de autenticidad incluido",
            "Almacenamiento seguro por 1 año",
            "Soporte técnico incluido"
        ));
        servicioRepository.save(servicio1);

        Servicio servicio2 = new Servicio();
        servicio2.setNombre("Firma Digital Avanzada");
        servicio2.setDescripcion("Firma digital avanzada con múltiples firmantes y validación");
        servicio2.setDescripcionCompleta("<h2>Firma Digital Avanzada</h2><p>Servicio completo de firma digital para documentos empresariales y legales complejos.</p><p><strong>Características:</strong></p><ul><li>Múltiples firmantes</li><li>Validación biométrica</li><li>Almacenamiento ilimitado</li><li>Auditoría completa</li></ul>");
        servicio2.setPrecio(35000);
        servicio2.setDetalles(Arrays.asList(
            "Múltiples firmantes permitidos",
            "Validación biométrica opcional",
            "Almacenamiento ilimitado",
            "Auditoría completa de transacciones",
            "Soporte prioritario 24/7"
        ));
        servicioRepository.save(servicio2);

        Servicio servicio3 = new Servicio();
        servicio3.setNombre("Certificación de Documentos");
        servicio3.setDescripcion("Certificación y validación de documentos oficiales");
        servicio3.setDescripcionCompleta("<h2>Certificación de Documentos</h2><p>Servicio especializado para certificar y validar documentos oficiales con validez legal.</p><p><strong>Beneficios:</strong></p><ul><li>Certificación notarial</li><li>Validación ante organismos públicos</li><li>Traducción certificada disponible</li></ul>");
        servicio3.setPrecio(25000);
        servicio3.setDetalles(Arrays.asList(
            "Certificación notarial incluida",
            "Validación ante organismos públicos",
            "Traducción certificada disponible",
            "Entrega en 24-48 horas",
            "Asesoría legal incluida"
        ));
        servicioRepository.save(servicio3);

        Servicio servicio4 = new Servicio();
        servicio4.setNombre("Firma Masiva de Documentos");
        servicio4.setDescripcion("Servicio para firmar múltiples documentos de forma eficiente");
        servicio4.setDescripcionCompleta("<h2>Firma Masiva de Documentos</h2><p>Ideal para empresas que necesitan firmar grandes volúmenes de documentos.</p><p><strong>Ventajas:</strong></p><ul><li>Procesamiento en lote</li><li>Descuentos por volumen</li><li>API de integración</li></ul>");
        servicio4.setPrecio(50000);
        servicio4.setDetalles(Arrays.asList(
            "Procesamiento en lote (hasta 100 documentos)",
            "Descuentos por volumen",
            "API de integración disponible",
            "Dashboard de seguimiento",
            "Soporte dedicado"
        ));
        servicioRepository.save(servicio4);

        Servicio servicio5 = new Servicio();
        servicio5.setNombre("Consultoría Legal Digital");
        servicio5.setDescripcion("Asesoría legal especializada en documentos digitales");
        servicio5.setDescripcionCompleta("<h2>Consultoría Legal Digital</h2><p>Servicio de consultoría especializada en legalidad de documentos digitales y firmas electrónicas.</p><p><strong>Incluye:</strong></p><ul><li>Revisión legal de documentos</li><li>Asesoría en cumplimiento normativo</li><li>Auditoría de procesos</li></ul>");
        servicio5.setPrecio(75000);
        servicio5.setDetalles(Arrays.asList(
            "Revisión legal completa",
            "Asesoría en cumplimiento normativo",
            "Auditoría de procesos digitales",
            "Informe detallado",
            "Sesión de consultoría de 2 horas"
        ));
        servicioRepository.save(servicio5);

        System.out.println("✅ Servicios de prueba insertados correctamente");
    }

    private void insertarCompras() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Servicio> servicios = servicioRepository.findAll();

        if (usuarios.isEmpty() || servicios.isEmpty()) {
            return;
        }

        Compra compra1 = new Compra();
        compra1.setUsuario(usuarios.get(1));
        compra1.setServicio(servicios.get(0));
        compra1.setFechaCompra(LocalDateTime.now().minusDays(10));
        compra1.setMonto(servicios.get(0).getPrecio());
        compraRepository.save(compra1);

        Compra compra2 = new Compra();
        compra2.setUsuario(usuarios.get(1));
        compra2.setServicio(servicios.get(2));
        compra2.setFechaCompra(LocalDateTime.now().minusDays(5));
        compra2.setMonto(servicios.get(2).getPrecio());
        compraRepository.save(compra2);

        Compra compra3 = new Compra();
        compra3.setUsuario(usuarios.get(2));
        compra3.setServicio(servicios.get(1));
        compra3.setFechaCompra(LocalDateTime.now().minusDays(3));
        compra3.setMonto(servicios.get(1).getPrecio());
        compraRepository.save(compra3);

        Compra compra4 = new Compra();
        compra4.setUsuario(usuarios.get(3));
        compra4.setServicio(servicios.get(4));
        compra4.setFechaCompra(LocalDateTime.now().minusDays(1));
        compra4.setMonto(servicios.get(4).getPrecio());
        compraRepository.save(compra4);

        System.out.println("✅ Compras de prueba insertadas correctamente");
    }
}

