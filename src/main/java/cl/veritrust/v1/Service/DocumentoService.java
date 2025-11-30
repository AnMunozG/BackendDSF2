package cl.veritrust.v1.Service;
import cl.veritrust.v1.Model.Documento;
import cl.veritrust.v1.Repository.DocumentoRepository;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DocumentoService {
    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private UsuarioService usuarioService;

    public List<Documento> ObtenerDocumentos() {
        return documentoRepository.findAll();
    }

    public Documento ObtenerDocumentoPorId(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con id: " + id));
    }

    public Documento CrearDocumento(Documento documento) {
        // Si la entidad llega con solo usuario.id (creada por toEntity), resolver el Usuario
        if (documento.getUsuario() != null && documento.getUsuario().getId() != null) {
            Usuario u = usuarioService.ObtenerUsuarioPorId(documento.getUsuario().getId());
            documento.setUsuario(u);
        }
        return documentoRepository.save(documento);
    }

    public Documento ActualizarDocumento(Long id, Documento detallesDocumento) {
        Documento documento = ObtenerDocumentoPorId(id);
        // actualizar campos permitidos
        if (detallesDocumento.getNombreOriginal() != null) documento.setNombreOriginal(detallesDocumento.getNombreOriginal());
        if (detallesDocumento.getTipoContenido() != null) documento.setTipoContenido(detallesDocumento.getTipoContenido());
        if (detallesDocumento.getTamano() != null) documento.setTamano(detallesDocumento.getTamano());
        if (detallesDocumento.getNombreFirmado() != null) documento.setNombreFirmado(detallesDocumento.getNombreFirmado());
        documento.setFirmado(detallesDocumento.isFirmado());
        // permitir actualizar el usuario asociado mediante detallesDocumento.usuario.id
        if (detallesDocumento.getUsuario() != null && detallesDocumento.getUsuario().getId() != null) {
            Usuario u = usuarioService.ObtenerUsuarioPorId(detallesDocumento.getUsuario().getId());
            documento.setUsuario(u);
        }
        return documentoRepository.save(documento);
    }

    public List<Documento> ObtenerDocumentosPorUsuario(Long usuarioId) {
        return documentoRepository.findByUsuarioId(usuarioId);
    }

    public void EliminarDocumento(Long id) {
        Documento documento = ObtenerDocumentoPorId(id);
        documentoRepository.delete(documento);
    }
}