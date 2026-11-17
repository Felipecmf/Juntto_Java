package br.com.juntto.controller;

import br.com.juntto.dao.CandidaturaDAO;
import br.com.juntto.dao.CategoriaDAO;
import br.com.juntto.dao.NotificacaoDAO;
import br.com.juntto.dao.PedidoDAO;
import br.com.juntto.dao.UsuarioDAO;
import br.com.juntto.dto.NotificacaoRequest;
import br.com.juntto.dto.NotificacaoResponse;
import br.com.juntto.dto.NotificacaoUpdateRequest;
import br.com.juntto.model.Notificacao;
import br.com.juntto.service.NotificacaoService;
import br.com.juntto.service.Notificador;
import br.com.juntto.service.PedidoService;
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
public class NotificacaoController {

    private NotificacaoService notificacaoService;

    public NotificacaoController(DataSource dataSource) {
        PedidoService pedidoService = new PedidoService(new PedidoDAO(dataSource), new CandidaturaDAO(dataSource),
                new UsuarioDAO(dataSource), new CategoriaDAO(dataSource), new Notificador(new NotificacaoDAO(dataSource)));
        this.notificacaoService = new NotificacaoService(new NotificacaoDAO(dataSource), new CandidaturaDAO(dataSource),
                new UsuarioDAO(dataSource), pedidoService);
    }

    // Lembrete de um participante da candidatura para o outro
    @PostMapping("/notificacoes")
    public ResponseEntity<NotificacaoResponse> criar(@Valid @RequestBody NotificacaoRequest pedido) {
        Notificacao criada = notificacaoService.criarLembrete(pedido);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.getId()).toUri();
        return ResponseEntity.created(location).body(Conversor.converter(criada, NotificacaoResponse.class));
    }

    // ?usuarioId= notificações do usuário | &lida=false só as não lidas
    @GetMapping("/notificacoes")
    public ResponseEntity<ArrayList<NotificacaoResponse>> listar(@RequestParam(value = "usuarioId", required = false) String usuarioId,
                                                                 @RequestParam(value = "lida", required = false) Boolean lida) {
        return ResponseEntity.ok(Conversor.converterLista(notificacaoService.listar(usuarioId, lida), NotificacaoResponse.class));
    }

    @GetMapping("/notificacoes/{id}")
    public ResponseEntity<NotificacaoResponse> buscar(@PathVariable("id") String id) {
        return ResponseEntity.ok(Conversor.converter(notificacaoService.buscar(id), NotificacaoResponse.class));
    }

    @PutMapping("/notificacoes/{id}")
    public ResponseEntity<NotificacaoResponse> atualizar(@PathVariable("id") String id,
                                                         @Valid @RequestBody NotificacaoUpdateRequest pedido) {
        return ResponseEntity.ok(Conversor.converter(notificacaoService.atualizar(id, pedido.getLida()), NotificacaoResponse.class));
    }

    @DeleteMapping("/notificacoes/{id}")
    public ResponseEntity<Void> remover(@PathVariable("id") String id) {
        notificacaoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
