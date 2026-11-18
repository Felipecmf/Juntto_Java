package br.com.juntto.controller;

import br.com.juntto.dao.CandidaturaDAO;
import br.com.juntto.dao.NotificacaoDAO;
import br.com.juntto.dao.PedidoDAO;
import br.com.juntto.dao.UsuarioDAO;
import br.com.juntto.dto.CandidaturaRequest;
import br.com.juntto.dto.CandidaturaResponse;
import br.com.juntto.dto.CandidaturaUpdateRequest;
import br.com.juntto.model.Candidatura;
import br.com.juntto.service.CandidaturaService;
import br.com.juntto.service.Notificador;
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
public class CandidaturaController {

    private CandidaturaService candidaturaService;

    public CandidaturaController(DataSource dataSource) {
        this.candidaturaService = new CandidaturaService(new CandidaturaDAO(dataSource), new PedidoDAO(dataSource),
                new UsuarioDAO(dataSource), new Notificador(new NotificacaoDAO(dataSource)));
    }

    // [GS] Rota aninhada 1:N: o prestador se candidata ao pedido (201 + Location)
    @PostMapping("/pedidos/{id}/candidaturas")
    public ResponseEntity<CandidaturaResponse> candidatar(@PathVariable("id") String idPedido,
                                                          @Valid @RequestBody CandidaturaRequest pedido) {
        Candidatura criada = candidaturaService.candidatar(idPedido, pedido.getUsuarioId());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/candidaturas/{id}")
                .buildAndExpand(criada.getId()).toUri();
        return ResponseEntity.created(location).body(Conversor.converter(criada, CandidaturaResponse.class));
    }

    // O solicitante vê todos os candidatos do pedido
    @GetMapping("/pedidos/{id}/candidaturas")
    public ResponseEntity<ArrayList<CandidaturaResponse>> listarPorPedido(@PathVariable("id") String idPedido,
                                                                          @RequestParam("usuarioId") String usuarioId) {
        return ResponseEntity.ok(Conversor.converterLista(candidaturaService.listarPorPedido(idPedido, usuarioId),
                CandidaturaResponse.class));
    }

    // ?usuarioId= minhas candidaturas | sem filtro = todas
    @GetMapping("/candidaturas")
    public ResponseEntity<ArrayList<CandidaturaResponse>> listar(@RequestParam(value = "usuarioId", required = false) String usuarioId) {
        return ResponseEntity.ok(Conversor.converterLista(candidaturaService.listar(usuarioId), CandidaturaResponse.class));
    }

    @GetMapping("/candidaturas/{id}")
    public ResponseEntity<CandidaturaResponse> buscar(@PathVariable("id") String id, @RequestParam("usuarioId") String usuarioId) {
        return ResponseEntity.ok(Conversor.converter(candidaturaService.buscar(id, usuarioId), CandidaturaResponse.class));
    }

    // Quem publicou aceita (ACEITA) ou recusa (RECUSADA) um candidato
    @PutMapping("/candidaturas/{id}")
    public ResponseEntity<CandidaturaResponse> atualizar(@PathVariable("id") String id, @Valid @RequestBody CandidaturaUpdateRequest pedido) {
        return ResponseEntity.ok(Conversor.converter(candidaturaService.atualizar(id, pedido.getUsuarioId(), pedido.getStatus()),
                CandidaturaResponse.class));
    }

    // O candidato desiste (precisa enviar confirmar=true)
    @DeleteMapping("/candidaturas/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable("id") String id, @RequestParam("usuarioId") String usuarioId,
                                         @RequestParam(value = "confirmar", required = false) Boolean confirmar) {
        candidaturaService.cancelar(id, usuarioId, confirmar);
        return ResponseEntity.noContent().build();
    }
}
