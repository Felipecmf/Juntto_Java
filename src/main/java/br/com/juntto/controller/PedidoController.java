package br.com.juntto.controller;

import br.com.juntto.dao.CandidaturaDAO;
import br.com.juntto.dao.CategoriaDAO;
import br.com.juntto.dao.NotificacaoDAO;
import br.com.juntto.dao.PedidoDAO;
import br.com.juntto.dao.UsuarioDAO;
import br.com.juntto.dto.PagamentoRequest;
import br.com.juntto.dto.PedidoRequest;
import br.com.juntto.dto.PedidoResponse;
import br.com.juntto.dto.StatusPedidoRequest;
import br.com.juntto.dto.UsuarioIdRequest;
import br.com.juntto.model.Pedido;
import br.com.juntto.service.Notificador;
import br.com.juntto.service.PedidoService;
import br.com.juntto.util.Conversor;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.sql.DataSource;

@RestController
public class PedidoController {

    private PedidoService pedidoService;

    // [GS] O Spring entrega o DataSource; os DAOs e services são criados com new
    public PedidoController(DataSource dataSource) {
        this.pedidoService = new PedidoService(new PedidoDAO(dataSource), new CandidaturaDAO(dataSource),
                new UsuarioDAO(dataSource), new CategoriaDAO(dataSource), new Notificador(new NotificacaoDAO(dataSource)));
    }

    // [GS] 201 Created + cabeçalho Location
    @PostMapping("/pedidos")
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest pedido) {
        Pedido criado = pedidoService.criar(Conversor.converter(pedido, Pedido.class));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.getId()).toUri();
        return ResponseEntity.created(location).body(Conversor.converter(criado, PedidoResponse.class));
    }

    // Lista de oportunidades (só Abertos), com filtros opcionais
    @GetMapping("/pedidos")
    public ResponseEntity<ArrayList<PedidoResponse>> listar(
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "idCategoria", required = false) String idCategoria,
            @RequestParam(value = "valorMin", required = false) Double valorMin,
            @RequestParam(value = "valorMax", required = false) Double valorMax,
            @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(Conversor.converterLista(
                pedidoService.listarAbertos(tipo, idCategoria, valorMin, valorMax, data), PedidoResponse.class));
    }

    // ?usuarioId= opcional: se for quem publicou ou o candidato escolhido, os e-mails de contato aparecem
    @GetMapping("/pedidos/{id}")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable("id") String id,
                                                 @RequestParam(value = "usuarioId", required = false) String usuarioId) {
        return ResponseEntity.ok(Conversor.converter(pedidoService.buscar(id, usuarioId), PedidoResponse.class));
    }

    @PutMapping("/pedidos/{id}")
    public ResponseEntity<PedidoResponse> atualizar(@PathVariable("id") String id, @Valid @RequestBody PedidoRequest pedido) {
        Pedido atualizado = pedidoService.atualizar(id, Conversor.converter(pedido, Pedido.class));
        return ResponseEntity.ok(Conversor.converter(atualizado, PedidoResponse.class));
    }

    @DeleteMapping("/pedidos/{id}")
    public ResponseEntity<Void> remover(@PathVariable("id") String id, @RequestParam("usuarioId") String usuarioId) {
        pedidoService.remover(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    // Andamento: EM_ANDAMENTO, AGUARDANDO_CONFIRMACAO, CONCLUIDO ou CANCELADO
    @PatchMapping("/pedidos/{id}/status")
    public ResponseEntity<PedidoResponse> alterarStatus(@PathVariable("id") String id, @Valid @RequestBody StatusPedidoRequest pedido) {
        return ResponseEntity.ok(Conversor.converter(pedidoService.alterarStatus(id, pedido), PedidoResponse.class));
    }

    @PostMapping("/pedidos/{id}/pagamento")
    public ResponseEntity<PedidoResponse> registrarPagamento(@PathVariable("id") String id, @Valid @RequestBody PagamentoRequest pedido) {
        return ResponseEntity.ok(Conversor.converter(pedidoService.registrarPagamento(id, pedido), PedidoResponse.class));
    }

    @PatchMapping("/pedidos/{id}/pagamento/confirmacao")
    public ResponseEntity<PedidoResponse> confirmarPagamento(@PathVariable("id") String id, @Valid @RequestBody UsuarioIdRequest pedido) {
        return ResponseEntity.ok(Conversor.converter(pedidoService.confirmarPagamento(id, pedido.getUsuarioId()), PedidoResponse.class));
    }
}
