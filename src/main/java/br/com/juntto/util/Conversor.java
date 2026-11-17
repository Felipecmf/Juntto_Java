package br.com.juntto.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;

import java.util.ArrayList;

// Converte DTO -> model e model -> DTO com o ModelMapper.
// Os campos são copiados pelo nome (STRICT): "titulo" vai para "titulo", "idCategoria" para "idCategoria"...
public class Conversor {

    private static final ModelMapper mapper = criar();

    private static ModelMapper criar() {
        ModelMapper m = new ModelMapper();
        m.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);
        return m;
    }

    public static <T> T converter(Object origem, Class<T> destino) {
        return mapper.map(origem, destino);
    }

    // Converte uma lista inteira (ex.: ArrayList<Pedido> -> ArrayList<PedidoResponse>)
    public static <T> ArrayList<T> converterLista(ArrayList<?> origem, Class<T> destino) {
        ArrayList<T> lista = new ArrayList<>();
        for (Object item : origem) {
            lista.add(mapper.map(item, destino));
        }
        return lista;
    }
}
