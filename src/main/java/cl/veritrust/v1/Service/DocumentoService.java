package cl.veritrust.v1.Service;
import cl.veritrust.v1.Model.Documento;
import cl.veritrust.v1.Repository.DocumentoRepository;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class DocumentoService {
    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<Documento> ObtenerDocumentos() {
        return documentoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Documento ObtenerDocumentoPorId(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con id: " + id));
    }

    public Documento CrearDocumento(Documento documento) {
        if (documento.getUsuario() != null && documento.getUsuario().getId() != null) {
            Usuario u = usuarioService.ObtenerUsuarioPorId(documento.getUsuario().getId());
            documento.setUsuario(u);
        }
        return documentoRepository.save(documento);
    }

    public Documento ActualizarDocumento(Long id, Documento detallesDocumento) {
        Documento documento = ObtenerDocumentoPorId(id);
        if (detallesDocumento.getNombreOriginal() != null) documento.setNombreOriginal(detallesDocumento.getNombreOriginal());
        if (detallesDocumento.getTipoContenido() != null) documento.setTipoContenido(detallesDocumento.getTipoContenido());
        if (detallesDocumento.getTamano() != null) documento.setTamano(detallesDocumento.getTamano());
        if (detallesDocumento.getNombreFirmado() != null) documento.setNombreFirmado(detallesDocumento.getNombreFirmado());
        documento.setFirmado(detallesDocumento.isFirmado());
        if (detallesDocumento.getHashDocumento() != null) documento.setHashDocumento(detallesDocumento.getHashDocumento());
        if (detallesDocumento.getFechaFirma() != null) documento.setFechaFirma(detallesDocumento.getFechaFirma());
        if (detallesDocumento.getRutaAlmacenamiento() != null) documento.setRutaAlmacenamiento(detallesDocumento.getRutaAlmacenamiento());
        if (detallesDocumento.getUsuario() != null && detallesDocumento.getUsuario().getId() != null) {
            Usuario u = usuarioService.ObtenerUsuarioPorId(detallesDocumento.getUsuario().getId());
            documento.setUsuario(u);
        }
        return documentoRepository.save(documento);
    }

    @Transactional(rollbackFor = Exception.class)
    public Documento CrearDocumentoFirmado(Documento documento) {
        if (documento.getUsuario() == null || documento.getUsuario().getId() == null) {
            throw new IllegalArgumentException("El usuario es requerido");
        }
        if (documento.getNombreAlmacenado() == null || documento.getNombreAlmacenado().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del archivo es requerido");
        }
        if (documento.getNombreOriginal() == null || documento.getNombreOriginal().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre original del archivo es requerido");
        }
        if (documento.getHashDocumento() == null || documento.getHashDocumento().trim().isEmpty()) {
            throw new IllegalArgumentException("El hash del documento es requerido");
        }
        if (documento.getFechaFirma() == null) {
            throw new IllegalArgumentException("La fecha de firma es requerida");
        }
        if (documento.getRutaAlmacenamiento() == null || documento.getRutaAlmacenamiento().trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta de almacenamiento es requerida");
        }
        
        if (documento.getUsuario().getId() != null) {
            Usuario usuario = usuarioService.ObtenerUsuarioPorId(documento.getUsuario().getId());
            documento.setUsuario(usuario);
        }
        
        documento.setFirmado(true);
        return documentoRepository.save(documento);
    }

    @Transactional(readOnly = true)
    public List<Documento> ObtenerDocumentosPorUsuario(Long usuarioId) {
        return documentoRepository.findByUsuario_Id(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<Documento> ObtenerDocumentosFirmadosPorUsuario(Long usuarioId) {
        return documentoRepository.findByUsuario_IdAndFirmadoTrueOrderByFechaFirmaDesc(usuarioId);
    }

    public void EliminarDocumento(Long id) {
        Documento documento = ObtenerDocumentoPorId(id);
        documentoRepository.delete(documento);
    }
}