package br.com.juntto.controller;

import br.com.juntto.dao.CandidaturaDAO;
import br.com.juntto.dao.CategoriaDAO;
import br.com.juntto.dao.NotificacaoDAO;
import br.com.juntto.dao.PedidoDAO;
import br.com.juntto.dao.UsuarioDAO;
import br.com.juntto.dto.CategoriaResponse;
import br.com.juntto.dto.LoginRequest;
import br.com.juntto.dto.PedidoResponse;
import br.com.juntto.dto.TagsRequest;
import br.com.juntto.dto.UsuarioRequest;
import br.com.juntto.dto.UsuarioResponse;
import br.com.juntto.dto.UsuarioUpdateRequest;
import br.com.juntto.model.Usuario;
import br.com.juntto.service.Notificador;
import br.com.juntto.service.PedidoService;
import br.com.juntto.service.UsuarioService;
import br.com.juntto.util.Conversor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import javax.sql.DataSource;

@RestController
public class UsuarioController {

    private UsuarioService usuarioService;
    private PedidoService pedidoService;

    // [GS] O Spring entrega o DataSource; os DAOs e services são criados com new
    public UsuarioController(DataSource dataSource) {
        this.usuarioService = new UsuarioService(new UsuarioDAO(dataSource), new PedidoDAO(dataSource),
                new CategoriaDAO(dataSource), new CandidaturaDAO(dataSource));
        this.pedidoService = new PedidoService(new PedidoDAO(dataSource), new CandidaturaDAO(dataSource),
                new UsuarioDAO(dataSource), new CategoriaDAO(dataSource), new Notificador(new NotificacaoDAO(dataSource)));
    }

    // [GS] Cadastro: 201 + Location
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody UsuarioRequest pedido) {
        Usuario criado = usuarioService.cadastrar(Conversor.converter(pedido, Usuario.class));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.getId()).toUri();
        return ResponseEntity.created(location).body(Conversor.converter(criado, UsuarioResponse.class));
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(@Valid @RequestBody LoginRequest pedido) {
        return ResponseEntity.ok(Conversor.converter(usuarioService.login(pedido.getEmail(), pedido.getSenha()), UsuarioResponse.class));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<ArrayList<UsuarioResponse>> listar() {
        return ResponseEntity.ok(Conversor.converterLista(usuarioService.listar(), UsuarioResponse.class));
    }

    // ?usuarioId= igual ao id: todos os dados (meu perfil); senão, perfil público
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponse> buscar(@PathVariable("id") String id,
                                                  @RequestParam(value = "usuarioId", required = false) String usuarioId) {
        return ResponseEntity.ok(Conversor.converter(usuarioService.buscar(id, usuarioId), UsuarioResponse.class));
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable("id") String id, @Valid @RequestBody UsuarioUpdateRequest pedido) {
        Usuario atualizado = usuarioService.atualizar(id, pedido.getUsuarioId(), Conversor.converter(pedido, Usuario.class));
        return ResponseEntity.ok(Conversor.converter(atualizado, UsuarioResponse.class));
    }

    // ?usuarioId= precisa ser o próprio id (só o dono exclui a conta)
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> remover(@PathVariable("id") String id, @RequestParam("usuarioId") String usuarioId) {
        usuarioService.remover(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuarios/{id}/tags")
    public ResponseEntity<ArrayList<CategoriaResponse>> tags(@PathVariable("id") String id) {
        return ResponseEntity.ok(Conversor.converterLista(usuarioService.obterTags(id), CategoriaResponse.class));
    }

    @PutMapping("/usuarios/{id}/tags")
    public ResponseEntity<ArrayList<CategoriaResponse>> salvarTags(@PathVariable("id") String id, @Valid @RequestBody TagsRequest pedido) {
        return ResponseEntity.ok(Conversor.converterLista(usuarioService.salvarTags(id, pedido.getUsuarioId(), pedido.getIdsCategorias()), CategoriaResponse.class));
    }

    // [GS] Rota aninhada 1:N. ?papel=SOLICITANTE (pedidos que publiquei) ou PRESTADOR (pedidos em que me candidatei)
    @GetMapping("/usuarios/{id}/pedidos")
    public ResponseEntity<ArrayList<PedidoResponse>> pedidos(@PathVariable("id") String id,
                                                            @RequestParam(value = "papel", required = false) String papel) {
        return ResponseEntity.ok(Conversor.converterLista(pedidoService.listarDoUsuario(id, papel), PedidoResponse.class));
    }
}
