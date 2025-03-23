import java.io.*;

public class CompressaoDeDados {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java CompressaoDeDados <arquivo_entrada> <arquivo_saida>");
            return;
        }

        try (BufferedReader scanner = new BufferedReader(new FileReader(args[0]));
             BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]))) {

            String linha;
            while ((linha = scanner.readLine()) != null) {
                String[] dados = linha.split(" ");
                int tamanho = Integer.parseInt(dados[0]);

                // Ajuste do tamanho para evitar erro de índice
                int tamanhoReal = Math.min(tamanho, dados.length - 1);

                String[] sequencia = new String[tamanhoReal];
                System.arraycopy(dados, 1, sequencia, 0, tamanhoReal);

                int tamanhoOriginal = tamanhoReal * 8;

                Huffman huffman = new Huffman(sequencia);
                int tamanhoHuffman = huffman.obterTamanhoComprimido();
                double taxaCompressaoHuffman = ((double) (tamanhoOriginal - tamanhoHuffman) / tamanhoOriginal) * 100;
                String codigoHuffman = huffman.codificar();

                RLE rle = new RLE(sequencia);
                int tamanhoRLE = rle.obterTamanhoComprimido();
                double taxaCompressaoRLE = ((double) (tamanhoOriginal - tamanhoRLE) / tamanhoOriginal) * 100;
                String codigoRLE = rle.codificar();

                if (tamanhoHuffman < tamanhoRLE) {
                    writer.write(String.format("%d->HUF(%.2f%%)=%s\n", tamanhoReal, taxaCompressaoHuffman, codigoHuffman));
                } else if (tamanhoRLE < tamanhoHuffman) {
                    writer.write(String.format("%d->RLE(%.2f%%)=%s\n", tamanhoReal, taxaCompressaoRLE, codigoRLE));
                } else {
                    writer.write(String.format("%d->HUF(%.2f%%)=%s\n", tamanhoReal, taxaCompressaoHuffman, codigoHuffman));
                    writer.write(String.format("%d->RLE(%.2f%%)=%s\n", tamanhoReal, taxaCompressaoRLE, codigoRLE));
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar o arquivo: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Erro: formato inválido no arquivo de entrada.");
        }
    }
}


class Huffman {
    private final String[] sequencia;
    private final NoHuffman[] noArray;
    private int tamanho;

    public Huffman(String[] sequencia) {
        this.sequencia = sequencia;
        this.noArray = new NoHuffman[256];
        construirArvore();
    }

    private void construirArvore() {
        int[] frequencias = new int[256];
        for (String simbolo : sequencia) {
            frequencias[simbolo.hashCode() % 256]++;
        }

        for (int i = 0; i < 256; i++) {
            if (frequencias[i] > 0) {
                noArray[tamanho++] = new NoHuffman(frequencias[i], (char) i);
            }
        }

        while (tamanho > 1) {
            NoHuffman menor = extrairMinimo();
            NoHuffman segundoMenor = extrairMinimo();

            NoHuffman novoNo = new NoHuffman(menor.frequencia + segundoMenor.frequencia, '\0');
            novoNo.esquerda = menor;
            novoNo.direita = segundoMenor;

            inserirNo(novoNo);
        }

        if (tamanho == 1) {
            NoHuffman raiz = noArray[0];
            gerarCodigos(raiz, "");
        }

        // Verificar se todos os nós têm códigos gerados
        for (NoHuffman no : noArray) {
            if (no != null && no.codigo == null) {
                gerarCodigos(no, "");
            }
        }
    }

    private void inserirNo(NoHuffman no) {
        noArray[tamanho++] = no;
        int i = tamanho - 1;
        while (i > 0 && noArray[i].frequencia < noArray[(i - 1) / 2].frequencia) {
            NoHuffman temp = noArray[i];
            noArray[i] = noArray[(i - 1) / 2];
            noArray[(i - 1) / 2] = temp;
            i = (i - 1) / 2;
        }
    }

    private NoHuffman extrairMinimo() {
        NoHuffman min = noArray[0];
        noArray[0] = noArray[--tamanho];
        heapify(0);
        return min;
    }

    private void heapify(int i) {
        int menor = i;
        int esquerda = 2 * i + 1;
        int direita = 2 * i + 2;
        if (esquerda < tamanho && noArray[esquerda].frequencia < noArray[menor].frequencia) {
            menor = esquerda;
        }
        if (direita < tamanho && noArray[direita].frequencia < noArray[menor].frequencia) {
            menor = direita;
        }
        if (menor != i) {
            NoHuffman temp = noArray[i];
            noArray[i] = noArray[menor];
            noArray[menor] = temp;
            heapify(menor);
        }
    }

    private void gerarCodigos(NoHuffman no, String codigo) {
        if (no == null) return;
        if (no.esquerda == null && no.direita == null) {
            no.codigo = codigo;
            return;
        }
        gerarCodigos(no.esquerda, codigo + "0");
        gerarCodigos(no.direita, codigo + "1");
    }

    public int obterTamanhoComprimido() {
        int tamanho = 0;
        for (String simbolo : sequencia) {
            int index = simbolo.hashCode() % 256;
            if (noArray[index] != null && noArray[index].codigo != null) {
                tamanho += noArray[index].codigo.length();
            }
        }
        return tamanho;
    }

    public String codificar() {
        StringBuilder comprimido = new StringBuilder();
        for (String simbolo : sequencia) {
            int index = simbolo.hashCode() % 256;
            if (noArray[index] != null && noArray[index].codigo != null) {
                comprimido.append(noArray[index].codigo);
            }
        }
        return binarioParaHexadecimal(comprimido.toString());
    }

    private String binarioParaHexadecimal(String binario) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < binario.length(); i += 4) {
            String grupo = binario.substring(i, Math.min(i + 4, binario.length()));
            int decimal = Integer.parseInt(grupo, 2);
            hex.append(Integer.toHexString(decimal).toUpperCase());
        }
        return hex.toString();
    }

    private static class NoHuffman {
        int frequencia;
        char simbolo;
        NoHuffman esquerda, direita;
        String codigo;

        NoHuffman(int frequencia, char simbolo) {
            this.frequencia = frequencia;
            this.simbolo = simbolo;
            this.esquerda = null;
            this.direita = null;
            this.codigo = null;
        }
    }
}

class RLE {
    private final String[] sequencia;

    public RLE(String[] sequencia) {
        this.sequencia = sequencia;
    }

    public int obterTamanhoComprimido() {
        String comprimido = codificar();
        return comprimido.length() * 4;
    }

    public String codificar() {
        if (sequencia.length == 0) {
            return ""; // Retorna vazio para evitar erro
        }

        StringBuilder comprimido = new StringBuilder();
        int contador = 1;
        
        for (int i = 1; i < sequencia.length; i++) {
            if (sequencia[i].equals(sequencia[i - 1])) {
                contador++;
            } else {
                comprimido.append(String.format("%02X", contador)).append(sequencia[i - 1]);
                contador = 1;
            }
        }

        // Adiciona o último símbolo e sua contagem
        comprimido.append(String.format("%02X", contador)).append(sequencia[sequencia.length - 1]);

        return comprimido.toString();
    }
}

