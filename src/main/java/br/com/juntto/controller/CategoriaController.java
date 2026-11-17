package br.com.juntto.controller;

import br.com.juntto.dao.CategoriaDAO;
import br.com.juntto.dto.CategoriaRequest;
import br.com.juntto.dto.CategoriaResponse;
import br.com.juntto.model.Categoria;
import br.com.juntto.service.CategoriaService;
import br.com.juntto.util.Conversor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import javax.sql.DataSource;

@RestController
public class CategoriaController {

    private CategoriaService categoriaService;

    public CategoriaController(DataSource dataSource) {
        this.categoriaService = new CategoriaService(new CategoriaDAO(dataSource));
    }

    @GetMapping("/categorias")
    public ResponseEntity<ArrayList<CategoriaResponse>> listar() {
        return ResponseEntity.ok(Conversor.converterLista(categoriaService.listar(), CategoriaResponse.class));
    }

    @GetMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponse> buscar(@PathVariable("id") String id) {
        return ResponseEntity.ok(Conversor.converter(categoriaService.buscar(id), CategoriaResponse.class));
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest pedido) {
        Categoria criada = categoriaService.criar(pedido.getNome());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.getId()).toUri();
        return ResponseEntity.created(location).body(Conversor.converter(criada, CategoriaResponse.class));
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponse> atualizar(@PathVariable("id") String id, @Valid @RequestBody CategoriaRequest pedido) {
        return ResponseEntity.ok(Conversor.converter(categoriaService.atualizar(id, pedido.getNome()), CategoriaResponse.class));
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> remover(@PathVariable("id") String id) {
        categoriaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
