package cl.veritrust.v1.Service;

import cl.veritrust.v1.Exception.ResourceNotFoundException;
import cl.veritrust.v1.Model.DocumentoFirmado;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Repository.DocumentoFirmadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DocumentoFirmadoService {
    
    @Autowired
    private DocumentoFirmadoRepository documentoFirmadoRepository;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Transactional(readOnly = true)
    public List<DocumentoFirmado> obtenerDocumentosFirmadosPorUsuario(Long usuarioId) {
        return documentoFirmadoRepository.findByUsuario_IdOrderByFechaFirmaDesc(usuarioId);
    }
    
    @Transactional(readOnly = true)
    public DocumentoFirmado obtenerDocumentoFirmadoPorId(Long id) {
        return documentoFirmadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento firmado no encontrado con id: " + id));
    }
    
    @Transactional(rollbackFor = Exception.class)
    public DocumentoFirmado crearDocumentoFirmado(DocumentoFirmado documentoFirmado) {
        // Resolver la relación con Usuario si solo viene el ID
        if (documentoFirmado.getUsuario() != null && documentoFirmado.getUsuario().getId() != null) {
            Usuario usuario = usuarioService.ObtenerUsuarioPorId(documentoFirmado.getUsuario().getId());
            documentoFirmado.setUsuario(usuario);
        } else {
            throw new IllegalArgumentException("El usuario es requerido");
        }
        
        // Validar que todos los campos requeridos estén presentes
        if (documentoFirmado.getNombreArchivo() == null || documentoFirmado.getNombreArchivo().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del archivo es requerido");
        }
        if (documentoFirmado.getNombreOriginal() == null || documentoFirmado.getNombreOriginal().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre original del archivo es requerido");
        }
        if (documentoFirmado.getHashDocumento() == null || documentoFirmado.getHashDocumento().trim().isEmpty()) {
            throw new IllegalArgumentException("El hash del documento es requerido");
        }
        if (documentoFirmado.getFechaFirma() == null) {
            throw new IllegalArgumentException("La fecha de firma es requerida");
        }
        if (documentoFirmado.getRutaAlmacenamiento() == null || documentoFirmado.getRutaAlmacenamiento().trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta de almacenamiento es requerida");
        }
        
        System.out.println("Guardando documento firmado en BD: " + documentoFirmado.getNombreArchivo());
        return documentoFirmadoRepository.save(documentoFirmado);
    }
    
    @Transactional(readOnly = true)
    public boolean documentoPerteneceAUsuario(Long documentoId, Long usuarioId) {
        DocumentoFirmado documento = obtenerDocumentoFirmadoPorId(documentoId);
        return documento.getUsuario().getId().equals(usuarioId);
    }
}
