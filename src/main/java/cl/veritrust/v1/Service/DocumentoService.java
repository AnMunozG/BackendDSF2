package cl.veritrust.v1.Service;
import cl.veritrust.v1.Model.Documento;
import cl.veritrust.v1.Repository.DocumentoRepository;
import cl.veritrust.v1.Exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DocumentoService {
    @Autowired
    private DocumentoRepository documentoRepository;

    public List<Documento> ObtenerDocumentos() {
        return documentoRepository.findAll();
    }

    public Documento ObtenerDocumentoPorId(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con id: " + id));
    }

    public Documento CrearDocumento(Documento documento) {
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