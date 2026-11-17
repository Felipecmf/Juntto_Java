package br.com.juntto.controller;

import br.com.juntto.dao.CandidaturaDAO;
import br.com.juntto.dao.FeedbackDAO;
import br.com.juntto.dao.NotificacaoDAO;
import br.com.juntto.dao.PedidoDAO;
import br.com.juntto.dto.FeedbackRequest;
import br.com.juntto.dto.FeedbackResponse;
import br.com.juntto.model.Feedback;
import br.com.juntto.service.FeedbackService;
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
public class FeedbackController {

    private FeedbackService feedbackService;

    public FeedbackController(DataSource dataSource) {
        this.feedbackService = new FeedbackService(new FeedbackDAO(dataSource), new PedidoDAO(dataSource),
                new CandidaturaDAO(dataSource), new Notificador(new NotificacaoDAO(dataSource)));
    }

    // [GS] Rota aninhada 1:N: quem pediu ou quem ajudou avalia o serviço (estrelas de 1 a 5 + texto)
    @PostMapping("/pedidos/{id}/feedbacks")
    public ResponseEntity<FeedbackResponse> avaliar(@PathVariable("id") String idPedido, @Valid @RequestBody FeedbackRequest pedido) {
        Feedback feedback = feedbackService.avaliar(idPedido, pedido);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/feedbacks/{id}")
                .buildAndExpand(feedback.getId()).toUri();
        return ResponseEntity.created(location).body(Conversor.converter(feedback, FeedbackResponse.class));
    }

    @GetMapping("/pedidos/{id}/feedbacks")
    public ResponseEntity<ArrayList<FeedbackResponse>> listarPorPedido(@PathVariable("id") String idPedido) {
        return ResponseEntity.ok(Conversor.converterLista(feedbackService.listarPorPedido(idPedido), FeedbackResponse.class));
    }

    // ?usuarioId= avaliações recebidas (perfil) | sem filtro = todas
    @GetMapping("/feedbacks")
    public ResponseEntity<ArrayList<FeedbackResponse>> listar(@RequestParam(value = "usuarioId", required = false) String usuarioId) {
        return ResponseEntity.ok(Conversor.converterLista(feedbackService.listar(usuarioId), FeedbackResponse.class));
    }

    @GetMapping("/feedbacks/{id}")
    public ResponseEntity<FeedbackResponse> buscar(@PathVariable("id") String id) {
        return ResponseEntity.ok(Conversor.converter(feedbackService.buscar(id), FeedbackResponse.class));
    }

    @PutMapping("/feedbacks/{id}")
    public ResponseEntity<FeedbackResponse> atualizar(@PathVariable("id") String id, @Valid @RequestBody FeedbackRequest pedido) {
        return ResponseEntity.ok(Conversor.converter(feedbackService.atualizar(id, pedido), FeedbackResponse.class));
    }

    @DeleteMapping("/feedbacks/{id}")
    public ResponseEntity<Void> remover(@PathVariable("id") String id, @RequestParam("usuarioId") String usuarioId) {
        feedbackService.remover(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
