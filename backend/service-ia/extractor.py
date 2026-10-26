import io
from pypdf import PdfReader
import docx

def extract_text_from_file(file_bytes: bytes, filename: str) -> str:
    """Extrae texto limpio desde archivos PDF, DOCX o TXT."""
    filename_lower = filename.lower()
    
    if filename_lower.endswith(".pdf"):
        reader = PdfReader(io.BytesIO(file_bytes))
        pages_text = []
        for i, page in enumerate(reader.pages):
            text = page.extract_text()
            if text:
                pages_text.append(f"--- Página {i+1} ---\n{text.strip()}")
        return "\n\n".join(pages_text)
        
    elif filename_lower.endswith(".docx"):
        doc = docx.Document(io.BytesIO(file_bytes))
        paragraphs = [p.text.strip() for p in doc.paragraphs if p.text.strip()]
        return "\n\n".join(paragraphs)
        
    elif filename_lower.endswith((".txt", ".md", ".csv")):
        try:
            return file_bytes.decode("utf-8")
        except UnicodeDecodeError:
            return file_bytes.decode("latin-1", errors="ignore")
            
    else:
        # Intentar decodificar como texto por defecto
        try:
            return file_bytes.decode("utf-8")
        except Exception:
            raise ValueError(f"Formato de archivo no soportado: {filename}. Usa PDF, Word (.docx) o TXT.")
