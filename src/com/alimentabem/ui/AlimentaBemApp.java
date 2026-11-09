package com.alimentabem.ui;

import com.alimentabem.controller.Fachada;
import com.alimentabem.model.*;
import com.alimentabem.service.NutricaoService;
import com.alimentabem.service.RelatorioService;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AlimentaBem — interface gráfica (Swing) do sistema de banco de alimentos.
 *
 * Como executar: rode o método main desta classe. (A interface de console continua em {@code Main}.)
 *
 * Onde está cada requisito nesta tela (procure por "// REQxx" no código):
 *   REQ01  doadores ................ abrirTelaDoadores / abrirCadastroDoador
 *   REQ02  instituições + CEP ...... abrirCadastroInstituicao / consultarCep
 *   REQ03  alimentos ............... abrirTelaAlimentos / abrirFormAlimento
 *   REQ04  doação + itens .......... abrirCadastroDoacao / abrirAdicionarItemDoacao
 *   REQ05  valor nutricional (USDA)  abrirValorNutricional / consultarUsda
 *   REQ06  calorias/proteínas ...... nutriDoacao (total ao vivo em Nova Doação)
 *   REQ07  estoque por lote ........ abrirTelaEstoque
 *   REQ08  distribuição ............ abrirCadastroDistribuicao / validarERegistrarDistribuicao
 *   REQ09  sugestão FIFO ........... abrirSugestaoFIFO / planejarFIFO
 *   REQ10  relatório CSV ........... abrirRelatorioDoacoes
 *   REQ11  relatório nutricional ... abrirRelatorioNutricional
 *   REQ12  estoque a vencer (7d) ... abrirEstoqueAVencer / lotesAVencer
 *   REQ13  bloqueia lote vencido ... planejarFIFO / validarERegistrarDistribuicao
 *   REQ14  CEP inválido ............ abrirCadastroInstituicao / consultarCep
 *   REQ15  doação sem itens ........ abrirCadastroDoacao
 *   REQ16  qtd. acima do estoque ... abrirAdicionarItemDistribuicao / planejarFIFO
 *   REQ17  capacidade mensal ....... validarERegistrarDistribuicao
 *   REQ18  FIFO sempre primeiro .... abrirSugestaoFIFO / planejarFIFO
 *   REQ19  doação com item vencido . abrirAdicionarItemDoacao
 *   REQ20  excluir instituição ..... abrirTelaInstituicoes
 *   BÔNUS  relatório HTML .......... gerarRelatorioHtml
 *
 * As regras (REQ13 a REQ20) também são validadas pela camada de serviço, chamada via {@link Fachada};
 * a tela apenas antecipa as mensagens para o usuário.
 */
public class AlimentaBemApp extends JFrame {

    private final Fachada fachada = new Fachada();

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    // STRICT + 'uuuu': rejeita datas impossíveis como 31/02 (antes viravam 28/02 silenciosamente)
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6)).build();

    // Cartões de estatística da tela principal
    private final JLabel statDoadores = new JLabel("0");
    private final JLabel statAlimentos = new JLabel("0");
    private final JLabel statLotes = new JLabel("0");
    private final JLabel statVencer = new JLabel("0");

    // =====================================================================
    // TEMA
    // =====================================================================
    static final class Theme {
        // Paleta "Sattis": 00606c / 019391 / 00dfc3 / ffffff + azul-marinho de apoio
        static final Color NAVY = new Color(0x2A3550);
        static final Color PRIMARY = new Color(0x019391);
        static final Color PRIMARY_DARK = new Color(0x00606C);
        static final Color PRIMARY_SOFT = new Color(0xDDF6F2);
        static final Color ACCENT = new Color(0x00DFC3);
        static final Color BG = new Color(0xF0F6F7);
        static final Color CARD = Color.WHITE;
        static final Color TEXT = new Color(0x1E293B);
        static final Color MUTED = new Color(0x64748B);
        static final Color BORDER = new Color(0xD8E6E9);
        static final Color DANGER = new Color(0xDC2626);
        static final Color WARN = new Color(0xB45309);
        static final Color OK = new Color(0x0B8F78);

        static final String TITLE_FAMILY = pick("Poppins", "Montserrat", "Segoe UI Semibold",
                "Segoe UI", "SF Pro Display", "Helvetica Neue", "Ubuntu", Font.SANS_SERIF);
        static final String BODY_FAMILY = pick("Inter", "Segoe UI", "SF Pro Text", "Roboto",
                "Helvetica Neue", "Ubuntu", Font.SANS_SERIF);

        static String pick(String... names) {
            Set<String> disponiveis = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String n : names) {
                if (disponiveis.contains(n)) return n;
            }
            return Font.SANS_SERIF;
        }

        static Font title(float size) { return new Font(TITLE_FAMILY, Font.BOLD, 12).deriveFont(size); }
        static Font body(float size) { return new Font(BODY_FAMILY, Font.PLAIN, 12).deriveFont(size); }
        static Font bodyBold(float size) { return new Font(BODY_FAMILY, Font.BOLD, 12).deriveFont(size); }
    }

    private static void instalarTema() {
        try {
            UIManager.put("nimbusBase", Theme.PRIMARY_DARK);
            UIManager.put("nimbusFocus", Theme.ACCENT);
            UIManager.put("nimbusSelectionBackground", Theme.PRIMARY);
            UIManager.put("control", Theme.BG);
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // segue com o visual padrão
        }
        FontUIResource corpo = new FontUIResource(Theme.body(13));
        for (String k : new String[]{"Label.font", "Button.font", "TextField.font", "TextArea.font",
                "ComboBox.font", "List.font", "Table.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "ToolTip.font", "FormattedTextField.font"}) {
            UIManager.put(k, corpo);
        }
        UIManager.put("TableHeader.font", new FontUIResource(Theme.bodyBold(13)));
        UIManager.put("OptionPane.background", Theme.BG);
        UIManager.put("Panel.background", Theme.BG);
    }

    // =====================================================================
    // COMPONENTES VISUAIS REUTILIZÁVEIS
    // =====================================================================
    static class ModernButton extends JButton {
        enum Kind { PRIMARY, SECONDARY, DANGER }

        private final Kind kind;
        private boolean hover;
        private boolean pressed;

        ModernButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setFont(Theme.bodyBold(13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 18, 9, 18));
            setForeground(kind == Kind.SECONDARY ? Theme.PRIMARY_DARK : Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c1, c2;
            switch (kind) {
                case DANGER: c1 = Theme.DANGER; c2 = new Color(0xF43F5E); break;
                case SECONDARY: c1 = Color.WHITE; c2 = Color.WHITE; break;
                default: c1 = Theme.PRIMARY_DARK; c2 = Theme.PRIMARY;
            }
            if (!isEnabled()) {
                c1 = new Color(0xC9D6D8);
                c2 = c1;
            } else if (pressed) {
                c1 = c1.darker();
                c2 = c2.darker();
            } else if (hover) {
                if (kind == Kind.SECONDARY) {
                    c1 = Theme.PRIMARY_SOFT;
                    c2 = Theme.PRIMARY_SOFT;
                } else if (kind == Kind.PRIMARY) {
                    c1 = Theme.PRIMARY;
                    c2 = new Color(0x00C2AA);
                } else {
                    c1 = c1.darker();
                    c2 = c2.darker();
                }
            }
            g2.setPaint(new GradientPaint(0, 0, c1, getWidth(), 0, c2));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            if (kind == Kind.SECONDARY) {
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Fundo em gradiente azul-marinho → verde-água, com formas translúcidas decorativas. */
    static class GradientPanel extends JPanel {
        GradientPanel() { setOpaque(false); }

        private static Path2D.Double onda(int w, int h, double base, double amp, double fase, double periodo) {
            Path2D.Double path = new Path2D.Double();
            path.moveTo(0, h);
            for (int x = 0; x <= w; x += 6) {
                path.lineTo(x, h * base + Math.sin(x / periodo + fase) * h * amp);
            }
            path.lineTo(w, h);
            path.closePath();
            return path;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, 0.55f, 1f},
                    new Color[]{Theme.NAVY, Theme.PRIMARY_DARK, Theme.PRIMARY}));
            g2.fillRect(0, 0, w, h);
            // formas translúcidas
            g2.setColor(new Color(0, 223, 195, 40));
            g2.fill(new Ellipse2D.Double(w * 0.70, -h * 0.9, h * 2.6, h * 2.6));
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fill(new Ellipse2D.Double(w * 0.87, h * 0.15, h * 1.7, h * 1.7));
            // ondas na base
            g2.setColor(new Color(0, 223, 195, 34));
            g2.fill(onda(w, h, 0.80, 0.07, 0.4, Math.max(40.0, w / 7.0)));
            g2.setColor(new Color(255, 255, 255, 26));
            g2.fill(onda(w, h, 0.88, 0.06, 2.2, Math.max(30.0, w / 5.0)));
            g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT, w, 0, new Color(0, 223, 195, 0)));
            g2.fillRect(0, h - 3, w, 3);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Painel branco com cantos arredondados e sombra suave (opcionalmente com barra de destaque). */
    static class RoundPanel extends JPanel {
        private final Color fill;
        private final boolean barra;

        RoundPanel(Color fill) { this(fill, false); }

        RoundPanel(Color fill, boolean barra) {
            this.fill = fill;
            this.barra = barra;
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 40, 50, 18));
            g2.fillRoundRect(1, 3, getWidth() - 3, getHeight() - 4, 18, 18);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 4, 18, 18);
            if (barra) {
                Shape antigo = g2.getClip();
                g2.setClip(new RoundRectangle2D.Double(0, 0, getWidth() - 3, getHeight() - 4, 18, 18));
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY_DARK, getWidth(), 0, Theme.ACCENT));
                g2.fillRect(0, 0, getWidth(), 5);
                g2.setClip(antigo);
            }
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 4, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class Badge extends JComponent {
        private final String chave;
        private final Color bg;
        private final Color fg;

        Badge(String chave, Color bg, Color fg) {
            this.chave = chave;
            this.bg = bg;
            this.fg = fg;
            setPreferredSize(new Dimension(46, 46));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillOval(0, 0, 44, 44);
            if (Icones.existe(chave)) {
                Icones.desenhar(g2, chave, 10, 10, 24, fg);
            } else {
                g2.setColor(fg);
                g2.setFont(Theme.title(18));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(chave, (44 - fm.stringWidth(chave)) / 2, (44 - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    /** Cartão clicável do menu principal, em quatro estilos. */
    static class MenuCard extends JPanel {
        static final int CLARO = 0, ESCURO = 1, GRADIENTE = 2, PERIGO = 3;
        private final int estilo;
        private boolean hover;

        MenuCard(String badge, String titulo, String descricao, int estilo, Runnable acao) {
            this.estilo = estilo;
            setLayout(new BorderLayout(14, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(18, 18, 18, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            Color bBg, bFg, tCor, dCor;
            switch (estilo) {
                case ESCURO:
                    bBg = new Color(0, 223, 195, 45); bFg = Theme.ACCENT;
                    tCor = Color.WHITE; dCor = new Color(0xB8C4D6); break;
                case GRADIENTE:
                    bBg = new Color(255, 255, 255, 60); bFg = Color.WHITE;
                    tCor = Color.WHITE; dCor = new Color(0xE0FBF7); break;
                case PERIGO:
                    bBg = new Color(0xFEE2E2); bFg = Theme.DANGER;
                    tCor = Theme.TEXT; dCor = Theme.MUTED; break;
                default:
                    bBg = Theme.PRIMARY_SOFT; bFg = Theme.PRIMARY_DARK;
                    tCor = Theme.TEXT; dCor = Theme.MUTED;
            }

            JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            badgeWrap.setOpaque(false);
            badgeWrap.add(new Badge(badge, bBg, bFg));
            add(badgeWrap, BorderLayout.WEST);

            JLabel t = new JLabel(titulo);
            t.setFont(Theme.title(16));
            t.setForeground(tCor);
            JLabel s = new JLabel("<html><body style='width:118px'>" + descricao + "</body></html>");
            s.setFont(Theme.body(12));
            s.setForeground(dCor);
            s.setVerticalAlignment(SwingConstants.TOP);
            JPanel txt = new JPanel(new BorderLayout(0, 6));
            txt.setOpaque(false);
            txt.add(t, BorderLayout.NORTH);
            txt.add(s, BorderLayout.CENTER);
            add(txt, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseReleased(MouseEvent e) {
                    if (contains(e.getPoint())) acao.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 3, h = getHeight() - 5;
            g2.setColor(estilo == CLARO || estilo == PERIGO
                    ? new Color(0, 40, 50, hover ? 34 : 16)
                    : new Color(0, 223, 195, hover ? 60 : 30));
            g2.fillRoundRect(1, 4, w, h, 22, 22);
            if (estilo == ESCURO) {
                g2.setPaint(new GradientPaint(0, 0, Theme.NAVY, w, h, new Color(0x161D33)));
            } else if (estilo == GRADIENTE) {
                g2.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, 0.65f, 1f},
                        new Color[]{Theme.PRIMARY_DARK, Theme.PRIMARY, new Color(0x00D2B8)}));
            } else {
                g2.setColor(Color.WHITE);
            }
            g2.fillRoundRect(0, 0, w, h, 22, 22);
            // rodapé "Abrir →"
            Color corRodape = estilo == ESCURO ? Theme.ACCENT : estilo == GRADIENTE ? Color.WHITE
                    : estilo == PERIGO ? Theme.DANGER : Theme.PRIMARY;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Theme.bodyBold(12));
            g2.setColor(corRodape);
            String rotulo = estilo == PERIGO ? "Encerrar" : "Abrir";
            int yb, ax;
            if (h >= 130) {                       // cartão alto: "Abrir →" no rodapé
                int xb = 18;
                yb = h - 18;
                g2.drawString(rotulo, xb, yb);
                ax = xb + g2.getFontMetrics().stringWidth(rotulo) + 8 + (hover ? 5 : 0);
            } else {                              // cartão baixo: só a seta, à direita
                yb = h / 2 + 4;
                ax = w - 40 + (hover ? 5 : 0);
            }
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(ax, yb - 4, ax + 12, yb - 4);
            g2.drawLine(ax + 8, yb - 8, ax + 12, yb - 4);
            g2.drawLine(ax + 8, yb, ax + 12, yb - 4);
            if (hover) {
                g2.setStroke(new BasicStroke(2.2f));
                g2.setColor(Theme.ACCENT);
                g2.drawRoundRect(1, 1, w - 2, h - 2, 22, 22);
            } else if (estilo == CLARO || estilo == PERIGO) {
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, w, h, 22, 22);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Ícones vetoriais de traço, desenhados em Java2D (sem imagens externas). */
    static final class Icones {
        static boolean existe(String chave) { return forma(chave) != null; }

        static void desenhar(Graphics2D g, String chave, double x, double y, double tamanho, Color cor) {
            Shape f = forma(chave);
            if (f == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            double k = tamanho / 24.0;
            g2.scale(k, k);
            g2.setColor(cor);
            g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(f);
            g2.dispose();
        }

        static Shape forma(String chave) {
            Path2D.Double p = new Path2D.Double();
            switch (chave) {
                case "doador":
                    p.append(new Ellipse2D.Double(8, 3.5, 8, 8), false);
                    p.moveTo(4, 21);
                    p.curveTo(4, 14.5, 20, 14.5, 20, 21);
                    break;
                case "alimento":
                    p.moveTo(12, 8);
                    p.curveTo(9, 5, 3, 7, 4, 14);
                    p.curveTo(5, 19.5, 9, 22, 12, 20.5);
                    p.curveTo(15, 22, 19, 19.5, 20, 14);
                    p.curveTo(21, 7, 15, 5, 12, 8);
                    p.moveTo(12, 8);
                    p.curveTo(12, 4.5, 14, 2.8, 16.5, 2.5);
                    p.curveTo(16.5, 5.5, 14.8, 7.6, 12, 8);
                    break;
                case "instituicao":
                    p.moveTo(3, 10); p.lineTo(12, 3); p.lineTo(21, 10); p.closePath();
                    p.moveTo(5, 10); p.lineTo(5, 20);
                    p.moveTo(19, 10); p.lineTo(19, 20);
                    p.moveTo(3, 20.5); p.lineTo(21, 20.5);
                    p.moveTo(9, 12.5); p.lineTo(9, 17.5);
                    p.moveTo(12, 12.5); p.lineTo(12, 17.5);
                    p.moveTo(15, 12.5); p.lineTo(15, 17.5);
                    break;
                case "estoque":
                    p.moveTo(3, 8); p.lineTo(12, 3); p.lineTo(21, 8); p.lineTo(21, 17); p.lineTo(12, 22); p.lineTo(3, 17); p.closePath();
                    p.moveTo(3, 8); p.lineTo(12, 13); p.lineTo(21, 8);
                    p.moveTo(12, 13); p.lineTo(12, 22);
                    break;
                case "doacao":
                    p.moveTo(12, 20.5);
                    p.curveTo(2, 13.5, 3.5, 5, 9, 5);
                    p.curveTo(11, 5, 12, 6.8, 12, 6.8);
                    p.curveTo(12, 6.8, 13, 5, 15, 5);
                    p.curveTo(20.5, 5, 22, 13.5, 12, 20.5);
                    p.closePath();
                    break;
                case "distribuicao":
                    p.moveTo(2, 6); p.lineTo(14, 6); p.lineTo(14, 17); p.lineTo(2, 17); p.closePath();
                    p.moveTo(14, 10); p.lineTo(18.5, 10); p.lineTo(22, 13.5); p.lineTo(22, 17); p.lineTo(14, 17);
                    p.append(new Ellipse2D.Double(5, 16.5, 4, 4), false);
                    p.append(new Ellipse2D.Double(16, 16.5, 4, 4), false);
                    break;
                case "relatorio":
                    p.moveTo(3.5, 20.5); p.lineTo(20.5, 20.5);
                    p.moveTo(6.5, 20.5); p.lineTo(6.5, 12);
                    p.moveTo(12, 20.5); p.lineTo(12, 5);
                    p.moveTo(17.5, 20.5); p.lineTo(17.5, 9.5);
                    break;
                case "sair":
                    p.append(new Arc2D.Double(5, 5, 14, 14, 125, 290, Arc2D.OPEN), false);
                    p.moveTo(12, 3); p.lineTo(12, 11.5);
                    break;
                case "relogio":
                    p.append(new Ellipse2D.Double(3, 3, 18, 18), false);
                    p.moveTo(12, 7); p.lineTo(12, 12); p.lineTo(15.5, 14);
                    break;
                default:
                    return null;
            }
            return p;
        }
    }

    /** Carrega e escala a logo do app (resources/logo.png). */
    static final class Marca {
        private static BufferedImage logo;
        private static boolean tentou;

        static synchronized BufferedImage logo() {
            if (!tentou) {
                tentou = true;
                try {
                    java.net.URL u = AlimentaBemApp.class.getResource("resources/logo.png");
                    if (u != null) {
                        logo = ImageIO.read(u);
                    } else {
                        File f = new File("src/com/alimentabem/ui/resources/logo.png");
                        if (f.exists()) logo = ImageIO.read(f);
                    }
                } catch (Exception ignored) {
                    // sem logo: o app usa um ícone desenhado no lugar
                }
            }
            return logo;
        }

        /** Reduz em etapas para manter a nitidez. */
        static BufferedImage escalada(int lado) {
            BufferedImage atual = logo();
            if (atual == null) return null;
            int w = atual.getWidth();
            while (w / 2 >= lado) {
                w /= 2;
                atual = redimensionar(atual, w, w);
            }
            return redimensionar(atual, lado, lado);
        }

        private static BufferedImage redimensionar(BufferedImage o, int w, int h) {
            BufferedImage r = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = r.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(o, 0, 0, w, h, null);
            g.dispose();
            return r;
        }
    }

    /** Selo branco arredondado com a logo, usado nos cabeçalhos. */
    static final class LogoChip extends JComponent {
        private final int lado;
        private final Image img;

        LogoChip(int lado) {
            this.lado = lado;
            this.img = Marca.escalada((int) (lado * 0.74));
            setPreferredSize(new Dimension(lado + 8, lado + 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = (int) (lado * 0.3);
            g2.setColor(new Color(0, 20, 30, 45));
            g2.fillRoundRect(4, 8, lado, lado, arc, arc);
            g2.setPaint(new GradientPaint(0, 0, Color.WHITE, 0, lado, new Color(0xE9FBF8)));
            g2.fillRoundRect(2, 2, lado, lado, arc, arc);
            if (img != null) {
                int s = img.getWidth(null);
                g2.drawImage(img, 2 + (lado - s) / 2, 2 + (lado - s) / 2, null);
            } else {
                Icones.desenhar(g2, "alimento", 2 + lado * 0.2, 2 + lado * 0.2, lado * 0.6, Theme.PRIMARY);
            }
            g2.dispose();
        }
    }

    /** Fundo da janela principal: cor base + brilhos radiais suaves. */
    static final class FundoPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(Theme.BG);
            g2.fillRect(0, 0, w, h);
            float r1 = Math.max(w, h) * 0.55f;
            g2.setPaint(new RadialGradientPaint(w * 0.92f, h * 0.35f, r1, new float[]{0f, 1f},
                    new Color[]{new Color(0, 223, 195, 38), new Color(0, 223, 195, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new RadialGradientPaint(w * 0.05f, h * 0.95f, r1, new float[]{0f, 1f},
                    new Color[]{new Color(1, 147, 145, 34), new Color(1, 147, 145, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

    /** Cápsula translúcida (efeito vidro) usada no cabeçalho para mostrar a data. */
    static final class PillInfo extends JComponent {
        private final String linha1;
        private final String linha2;

        PillInfo(String linha1, String linha2) {
            this.linha1 = linha1;
            this.linha2 = linha2;
            setPreferredSize(new Dimension(230, 64));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 60), 0, h, new Color(255, 255, 255, 22)));
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.drawRoundRect(0, 0, w, h, h, h);
            Icones.desenhar(g2, "relogio", 18, (h - 24) / 2.0, 24, Theme.ACCENT);
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.bodyBold(14));
            g2.drawString(linha1, 54, h / 2 - 3);
            g2.setColor(new Color(0xBFF5EE));
            g2.setFont(Theme.body(12));
            g2.drawString(linha2, 54, h / 2 + 15);
            g2.dispose();
        }
    }

    /** Barra de progresso com gradiente (teal → menta; âmbar acima de 80%; vermelha em 100%). */
    static void pintarBarra(Graphics2D g2, int x, int y, int w, int h, double frac) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0xE0ECEE));
        g2.fillRoundRect(x, y, w, h, h, h);
        if (frac <= 0) return;
        int fw = Math.max(h, (int) Math.round(w * Math.min(1.0, frac)));
        Color a, b;
        if (frac >= 1.0) { a = new Color(0xEF4444); b = new Color(0xB91C1C); }
        else if (frac >= 0.8) { a = new Color(0xFBBF24); b = new Color(0xD97706); }
        else { a = Theme.PRIMARY; b = Theme.ACCENT; }
        g2.setPaint(new GradientPaint(x, 0, a, x + fw, 0, b));
        g2.fillRoundRect(x, y, fw, h, h, h);
    }

    /** Célula de tabela em formato de cápsula colorida (status). */
    static final class PillLabel extends JLabel {
        private Color pillBg = Color.GRAY;
        private Color pillFg = Color.WHITE;
        private Color rowBg = Color.WHITE;

        void configurar(String texto, Color bg, Color fg, Color linha) {
            setText(texto);
            pillBg = bg;
            pillFg = fg;
            rowBg = linha;
            setFont(Theme.bodyBold(12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(rowBg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String t = getText();
            int maxTexto = getWidth() - 20 - 22;
            while (fm.stringWidth(t) > maxTexto && t.length() > 4) t = t.substring(0, t.length() - 2);
            if (!t.equals(getText())) t = t + "…";
            int w = fm.stringWidth(t) + 22, h = 24, x = 10, y = (getHeight() - h) / 2;
            g2.setColor(pillBg);
            g2.fillRoundRect(x, y, w, h, h, h);
            g2.setColor(pillFg);
            g2.drawString(t, x + 11, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Célula "usado / capacidade" com barra de progresso. */
    static final class BarraLabel extends JLabel {
        private long usado, cap;
        private Color rowBg = Color.WHITE;

        void configurar(long usado, long cap, Color linha) {
            this.usado = usado;
            this.cap = cap;
            this.rowBg = linha;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(rowBg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.TEXT);
            g2.setFont(Theme.bodyBold(12));
            g2.drawString(usado + " / " + cap + " famílias", 12, 15);
            pintarBarra(g2, 12, getHeight() - 13, getWidth() - 24, 6, cap == 0 ? 0 : (double) usado / cap);
            g2.dispose();
        }
    }

    /** Célula da lista de instituições: nome, saldo do mês e barra de capacidade (desenhada direto). */
    static final class CelulaInstituicao extends JComponent {
        private String nome = "";
        private String sub = "";
        private boolean esgotada;
        private double frac;
        private boolean selecionada;

        void configurar(String nome, String sub, boolean esgotada, double frac, boolean selecionada) {
            this.nome = nome;
            this.sub = sub;
            this.esgotada = esgotada;
            this.frac = frac;
            this.selecionada = selecionada;
        }

        @Override
        public Dimension getPreferredSize() { return new Dimension(10, 66); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(selecionada ? new Color(0xC8F0EA) : Color.WHITE);
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(0xEDF3F4));
            g2.drawLine(0, h - 1, w, h - 1);
            g2.setFont(Theme.bodyBold(13));
            g2.setColor(Theme.TEXT);
            String n = nome;
            FontMetrics fm = g2.getFontMetrics();
            while (fm.stringWidth(n) > w - 24 && n.length() > 4) n = n.substring(0, n.length() - 2);
            if (!n.equals(nome)) n += "…";
            g2.drawString(n, 12, 22);
            g2.setFont(Theme.body(11));
            g2.setColor(esgotada ? Theme.DANGER : Theme.MUTED);
            g2.drawString(sub, 12, 39);
            pintarBarra(g2, 12, h - 16, w - 24, 6, frac);
            g2.dispose();
        }
    }

    /** Combo flat e arredondado (independe do visual do Nimbus). */
    static final class ModernComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.PRIMARY_DARK);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                    g2.dispose();
                }
            };
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setOpaque(false);
            b.setPreferredSize(new Dimension(30, 28));
            return b;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) { }

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = c.getWidth(), h = c.getHeight();
            g2.setColor(c.isEnabled() ? Color.WHITE : new Color(0xE8F1F2));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g2.setColor(c.hasFocus() ? Theme.PRIMARY : Theme.BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g2.dispose();
            super.paint(g, c);
        }
    }

    /** Etiqueta arredondada (chip) para resumos. */
    static final class Chip extends JComponent {
        private final String texto;
        private final Color bg;
        private final Color fg;

        Chip(String texto, Color bg, Color fg) {
            this.texto = texto;
            this.bg = bg;
            this.fg = fg;
            setFont(Theme.bodyBold(12));
            FontMetrics fm = getFontMetrics(getFont());
            setPreferredSize(new Dimension(fm.stringWidth(texto) + 34, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(fg);
            g2.fillOval(11, getHeight() / 2 - 4, 8, 8);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(texto, 25, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Gráfico de barras horizontais com gradiente. */
    static final class GraficoBarras extends JComponent {
        private String titulo = "";
        private String unidade = "";
        private List<String> rotulos = new ArrayList<>();
        private List<Double> valores = new ArrayList<>();

        void dados(String titulo, String unidade, List<String> rotulos, List<Double> valores) {
            this.titulo = titulo;
            this.unidade = unidade;
            this.rotulos = rotulos;
            this.valores = valores;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setFont(Theme.title(13));
            g2.setColor(Theme.TEXT);
            g2.drawString(titulo, 0, 14);
            double max = valores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            if (valores.isEmpty() || max <= 0) {
                g2.setFont(Theme.body(13));
                g2.setColor(Theme.MUTED);
                String msg = "Nenhuma distribuição neste mês — registre uma para ver o gráfico.";
                g2.drawString(msg, (w - g2.getFontMetrics().stringWidth(msg)) / 2, h / 2 + 8);
                g2.dispose();
                return;
            }
            int n = valores.size();
            int topo = 26, labelW = 170, valW = 120;
            int linha = Math.min(32, (h - topo - 4) / Math.max(1, n));
            for (int i = 0; i < n; i++) {
                int y = topo + i * linha;
                g2.setFont(Theme.body(12));
                g2.setColor(Theme.TEXT);
                FontMetrics fm = g2.getFontMetrics();
                String r = rotulos.get(i);
                while (fm.stringWidth(r) > labelW - 12 && r.length() > 4) r = r.substring(0, r.length() - 2);
                if (!r.equals(rotulos.get(i))) r += "…";
                g2.drawString(r, 0, y + (linha + fm.getAscent() - fm.getDescent()) / 2);
                int bw = Math.max(40, w - labelW - valW);
                double frac = valores.get(i) / max;
                g2.setColor(new Color(0xE3EEF0));
                g2.fillRoundRect(labelW, y + (linha - 14) / 2, bw, 14, 14, 14);
                int fw = Math.max(14, (int) Math.round(bw * frac));
                g2.setPaint(new GradientPaint(labelW, 0, Theme.PRIMARY_DARK, labelW + fw, 0, Theme.ACCENT));
                g2.fillRoundRect(labelW, y + (linha - 14) / 2, fw, 14, 14, 14);
                g2.setColor(Theme.PRIMARY_DARK);
                g2.setFont(Theme.bodyBold(12));
                g2.drawString(nf(valores.get(i)) + " " + unidade, labelW + bw + 10, y + (linha + fm.getAscent() - fm.getDescent()) / 2);
            }
            g2.dispose();
        }
    }

    /** Linhas alternadas, destaque ao passar o mouse, cápsulas de status e barras de progresso. */
    static class ZebraRenderer extends DefaultTableCellRenderer {
        private final PillLabel pill = new PillLabel();
        private final BarraLabel barra = new BarraLabel();
        private static final Pattern PROPORCAO = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)\\s*famílias");

        static Color[] corStatus(String s) {
            if (s.startsWith("Vencido") || s.startsWith("Bloqueado") || s.startsWith("ESTOQUE"))
                return new Color[]{new Color(0xFEE2E2), new Color(0xB91C1C)};
            if (s.startsWith("Vence"))
                return new Color[]{new Color(0xFEF3C7), new Color(0x92400E)};
            if (s.startsWith("Válido") || s.startsWith("Sugerido"))
                return new Color[]{new Color(0xD3F6EF), new Color(0x00675A)};
            if (s.startsWith("Esgotado") || s.startsWith("Reserva"))
                return new Color[]{new Color(0xE5E9EC), new Color(0x475569)};
            return null;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, false, r, c);
            String s = v == null ? "" : v.toString();
            Color fundo;
            if (sel) {
                fundo = t.getSelectionBackground();
            } else {
                Object h = t.getClientProperty("hoverRow");
                fundo = (h instanceof Integer && (Integer) h == r) ? new Color(0xE3F6F3)
                        : (r % 2 == 0 ? Color.WHITE : new Color(0xF6FBFB));
            }
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setFont(Theme.body(13));
            setBackground(fundo);
            setForeground(Theme.TEXT);
            Color[] cores = corStatus(s);
            if (cores != null) {
                pill.setSize(t.getColumnModel().getColumn(c).getWidth(), t.getRowHeight());
                pill.configurar(s, cores[0], cores[1], fundo);
                return pill;
            }
            Matcher m = PROPORCAO.matcher(s);
            if (m.matches()) {
                barra.configurar(Long.parseLong(m.group(1)), Long.parseLong(m.group(2)), fundo);
                return barra;
            }
            return this;
        }
    }

    static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
            l.setOpaque(true);
            l.setBackground(Theme.NAVY);
            l.setForeground(Color.WHITE);
            l.setFont(Theme.bodyBold(12));
            l.setBorder(new EmptyBorder(0, 12, 0, 12));
            l.setHorizontalAlignment(SwingConstants.LEFT);
            return l;
        }
    }

    static class Form extends JPanel {
        private int row = 0;

        Form() {
            setLayout(new GridBagLayout());
            setOpaque(false);
        }

        void linha(String rotulo, JComponent campo) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = row;
            g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(6, 0, 6, 16);
            JLabel l = new JLabel(rotulo);
            l.setFont(Theme.bodyBold(12));
            l.setForeground(Theme.MUTED);
            super.add(l, g);
            g = new GridBagConstraints();
            g.gridx = 1;
            g.gridy = row;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.insets = new Insets(6, 0, 6, 0);
            super.add(campo, g);
            row++;
        }
    }

    // ---------- fábricas de componentes ----------
    private static void estilizar(JComponent c) {
        c.setFont(Theme.body(13));
        c.setBorder(new CompoundBorder(new LineBorder(Theme.BORDER, 1, true), new EmptyBorder(7, 10, 7, 10)));
    }

    private static JTextField campo(String texto) {
        JTextField f = new JTextField(texto);
        estilizar(f);
        return f;
    }

    private static JTextField campo() { return campo(""); }

    private static void habilitarCampo(JTextField f, boolean ativo) {
        f.setEditable(ativo);
        f.setBackground(ativo ? Color.WHITE : new Color(0xE8F1F2));
    }

    private static JTextField campoSomenteLeitura() {
        JTextField f = campo();
        f.setEditable(false);
        f.setBackground(new Color(0xE8F1F2));
        return f;
    }

    /** Campo que só aceita número decimal positivo (dígitos e um único "." ou ","). Letras são ignoradas ao digitar/colar. */
    private static JTextField campoDecimal(String texto) {
        JTextField f = campo(texto);
        restringirEntrada(f, "\\d*([.,]\\d*)?");
        return f;
    }

    /** Campo que só aceita número inteiro não negativo (somente dígitos). */
    private static JTextField campoInteiro() {
        JTextField f = campo();
        restringirEntrada(f, "\\d*");
        return f;
    }

    /** Barra qualquer edição cujo texto final não case com o padrão (e limita o tamanho a 12 caracteres). */
    private static void restringirEntrada(JTextField f, String regex) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            private boolean valido(FilterBypass fb, int offset, int length, String novo) throws BadLocationException {
                String atual = fb.getDocument().getText(0, fb.getDocument().getLength());
                String depois = atual.substring(0, offset) + (novo == null ? "" : novo) + atual.substring(offset + length);
                return depois.length() <= 12 && depois.matches(regex);
            }
            @Override public void insertString(FilterBypass fb, int offset, String str, javax.swing.text.AttributeSet a)
                    throws BadLocationException {
                if (valido(fb, offset, 0, str)) super.insertString(fb, offset, str, a);
                else Toolkit.getDefaultToolkit().beep();
            }
            @Override public void replace(FilterBypass fb, int offset, int length, String str, javax.swing.text.AttributeSet a)
                    throws BadLocationException {
                if (valido(fb, offset, length, str)) super.replace(fb, offset, length, str, a);
                else Toolkit.getDefaultToolkit().beep();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> JComboBox<T> combo(List<T> itens, Function<T, String> rotulo) {
        JComboBox<T> cb = new JComboBox<>(new Vector<>(itens));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                setText(v == null ? "" : rotulo.apply((T) v));
                setBorder(new EmptyBorder(5, 8, 5, 8));
                return this;
            }
        });
        cb.setFont(Theme.body(13));
        cb.setUI(new ModernComboBoxUI());
        cb.setOpaque(false);
        cb.setBackground(Color.WHITE);
        cb.setBorder(new EmptyBorder(2, 6, 2, 2));
        cb.setPreferredSize(new Dimension(200, 36));
        return cb;
    }

    private static JLabel rotuloMuted(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Theme.body(12));
        l.setForeground(Theme.MUTED);
        return l;
    }

    private static DefaultTableModel modeloTabela(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static void hoverLinha(JTable t, int linha) {
        Object antigo = t.getClientProperty("hoverRow");
        if (!(antigo instanceof Integer) || (Integer) antigo != linha) {
            t.putClientProperty("hoverRow", linha);
            t.repaint();
        }
    }

    private static JTable criarTabela(DefaultTableModel m) {
        JTable t = new JTable(m) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) { // estado vazio
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    String msg = "Nenhum registro por aqui ainda";
                    g2.setFont(Theme.bodyBold(14));
                    FontMetrics fm = g2.getFontMetrics();
                    int cx = getWidth() / 2, cy = Math.max(getHeight() / 2, 70);
                    Icones.desenhar(g2, "estoque", cx - 20, cy - 58, 40, new Color(0xB3CACE));
                    g2.setColor(Theme.MUTED);
                    g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy + 2);
                    g2.dispose();
                }
            }
        };
        t.setRowHeight(36);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setFont(Theme.body(13));
        t.setSelectionBackground(new Color(0xC8F0EA));
        t.setSelectionForeground(Theme.TEXT);
        t.setAutoCreateRowSorter(true);
        t.setDefaultRenderer(Object.class, new ZebraRenderer());
        for (int i = 0; i < m.getColumnCount(); i++) { // pesos de largura por tipo de coluna
            int peso;
            switch (m.getColumnName(i)) {
                case "Situação": peso = 190; break;
                case "Endereço": peso = 250; break;
                case "Atendidas no mês": peso = 200; break;
                case "E-mail": case "Instituição": peso = 190; break;
                case "Proteínas / 100g": peso = 135; break;
                case "Unidade": peso = 150; break;   // "Unidade (50 g)" é mais longo que "Litro (L)"
                case "CPF / CNPJ": peso = 190; break;
                case "Tipo": peso = 130; break;
                case "Fonte": peso = 190; break;
                case "Telefone": peso = 130; break;
                case "Nome": case "Doador": case "Alimento": peso = 160; break;
                case "Quantidade por instituição": peso = 210; break;
                case "Disponível (válido)": peso = 150; break;
                case "Lote": case "Prioridade": peso = 90; break;
                default: peso = 110;
            }
            t.getColumnModel().getColumn(i).setPreferredWidth(peso);
        }
        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hoverLinha(t, t.rowAtPoint(e.getPoint())); }
            @Override public void mouseExited(MouseEvent e) { hoverLinha(t, -1); }
        };
        t.addMouseMotionListener(hover);
        t.addMouseListener(hover);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 40));
        h.setDefaultRenderer(new HeaderRenderer());
        return t;
    }

    private static JScrollPane rolagem(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    private static JPanel cabecalho(String titulo, String subtitulo) {
        GradientPanel p = new GradientPanel();
        p.setLayout(new BorderLayout(16, 0));
        p.setBorder(new EmptyBorder(14, 22, 20, 24));
        p.add(new LogoChip(46), BorderLayout.WEST);
        JPanel textos = new JPanel(new GridLayout(0, 1, 0, 1));
        textos.setOpaque(false);
        JLabel t = new JLabel(titulo);
        t.setFont(Theme.title(21));
        t.setForeground(Color.WHITE);
        textos.add(t);
        if (subtitulo != null && !subtitulo.isEmpty()) {
            JLabel s = new JLabel(subtitulo);
            s.setFont(Theme.body(12));
            s.setForeground(new Color(0xBFF5EE));
            textos.add(s);
        }
        p.add(textos, BorderLayout.CENTER);
        return p;
    }

    private static JPanel rodapeBotoes() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        p.setOpaque(false);
        return p;
    }

    private static void aoDigitar(JTextField f, Runnable r) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { r.run(); }
            @Override public void removeUpdate(DocumentEvent e) { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    // ---------- mensagens ----------
    private static void aviso(Component pai, String msg) {
        JOptionPane.showMessageDialog(pai, msg, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    private static void erro(Component pai, String msg) {
        JOptionPane.showMessageDialog(pai, msg, "Não foi possível concluir", JOptionPane.ERROR_MESSAGE);
    }

    private static void sucesso(Component pai, String msg) {
        JOptionPane.showMessageDialog(pai, msg, "Tudo certo", JOptionPane.INFORMATION_MESSAGE);
    }

    private static boolean confirmar(Component pai, String msg) {
        return JOptionPane.showConfirmDialog(pai, msg, "Confirmar", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // =====================================================================
    // TELA DE LISTAGEM GENÉRICA (tabela + busca + botões)
    // =====================================================================
    private class ListScreen<T> {
        final JDialog dialog;
        final DefaultTableModel model;
        final JTable table;
        final JPanel footer = rodapeBotoes();
        private final Supplier<List<T>> source;
        private final Function<T, Object[]> mapper;
        private List<T> entities = new ArrayList<>();
        private final JPanel extra = new JPanel(new BorderLayout());
        private Runnable aposReload;

        ListScreen(String titulo, String subtitulo, String[] colunas,
                   Supplier<List<T>> source, Function<T, Object[]> mapper, int w, int h) {
            this.source = source;
            this.mapper = mapper;
            dialog = new JDialog(AlimentaBemApp.this, titulo, true);
            dialog.setSize(w, h);
            dialog.setLocationRelativeTo(AlimentaBemApp.this);

            model = modeloTabela(colunas);
            table = criarTabela(model);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(Theme.BG);
            root.add(cabecalho(titulo, subtitulo), BorderLayout.NORTH);

            JPanel corpo = new JPanel(new BorderLayout(0, 12));
            corpo.setOpaque(false);
            corpo.setBorder(new EmptyBorder(16, 22, 0, 22));

            JTextField busca = campo();
            aoDigitar(busca, () -> {
                String q = busca.getText().trim();
                @SuppressWarnings("unchecked")
                TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
                sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
            });
            JPanel top = new JPanel(new BorderLayout(10, 0));
            top.setOpaque(false);
            top.add(rotuloMuted("Buscar"), BorderLayout.WEST);
            top.add(busca, BorderLayout.CENTER);
            extra.setOpaque(false);
            JPanel norte = new JPanel(new BorderLayout(0, 10));
            norte.setOpaque(false);
            norte.add(top, BorderLayout.NORTH);
            norte.add(extra, BorderLayout.CENTER);
            corpo.add(norte, BorderLayout.NORTH);
            corpo.add(rolagem(table), BorderLayout.CENTER);

            root.add(corpo, BorderLayout.CENTER);
            footer.setBorder(new EmptyBorder(0, 12, 0, 12));
            root.add(footer, BorderLayout.SOUTH);
            dialog.setContentPane(root);

            ModernButton fechar = new ModernButton("Fechar", ModernButton.Kind.SECONDARY);
            fechar.addActionListener(e -> dialog.dispose());
            footer.add(fechar);
        }

        /** Adiciona um botão de ação; após executar, recarrega a tabela e as estatísticas. */
        ModernButton addAction(String texto, ModernButton.Kind tipo, Runnable acao) {
            ModernButton b = new ModernButton(texto, tipo);
            b.addActionListener(e -> {
                try {
                    acao.run();
                } catch (RuntimeException ex) { // erros de regra de negócio vindos da Fachada
                    erro(dialog, ex.getMessage() != null ? ex.getMessage() : ex.toString());
                }
                reload();
                refreshStats();
            });
            footer.add(b, footer.getComponentCount() - 1);
            return b;
        }

        void aoDuploClique(Runnable r) {
            table.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) r.run();
                }
            });
        }

        T selecionado() {
            int r = table.getSelectedRow();
            if (r < 0) return null;
            return entities.get(table.convertRowIndexToModel(r));
        }

        void reload() {
            entities = new ArrayList<>(source.get());
            model.setRowCount(0);
            for (T t : entities) model.addRow(mapper.apply(t));
            if (aposReload != null) aposReload.run();
        }

        void aoRecarregar(Runnable r) { this.aposReload = r; }

        void setExtra(JComponent c) {
            extra.removeAll();
            extra.add(c, BorderLayout.CENTER);
            extra.revalidate();
            extra.repaint();
        }

        void show() {
            reload();
            dialog.setVisible(true);
            refreshStats();
        }
    }

    /** Diálogo simples com cabeçalho, tabela de leitura e rodapé informativo. */
    private void dialogoTabela(String titulo, String subtitulo, String[] colunas, List<Object[]> linhas,
                               String rodapeHtml, int w, int h) {
        JDialog d = new JDialog(this, titulo, true);
        d.setSize(w, h);
        d.setLocationRelativeTo(this);
        DefaultTableModel m = modeloTabela(colunas);
        for (Object[] l : linhas) m.addRow(l);
        JTable t = criarTabela(m);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(cabecalho(titulo, subtitulo), BorderLayout.NORTH);
        JPanel corpo = new JPanel(new BorderLayout(0, 10));
        corpo.setOpaque(false);
        corpo.setBorder(new EmptyBorder(16, 22, 0, 22));
        corpo.add(rolagem(t), BorderLayout.CENTER);
        if (rodapeHtml != null) {
            JLabel l = new JLabel("<html>" + rodapeHtml + "</html>");
            l.setFont(Theme.bodyBold(13));
            l.setForeground(Theme.PRIMARY_DARK);
            corpo.add(l, BorderLayout.SOUTH);
        }
        root.add(corpo, BorderLayout.CENTER);
        JPanel rod = rodapeBotoes();
        ModernButton fechar = new ModernButton("Fechar", ModernButton.Kind.SECONDARY);
        fechar.addActionListener(e -> d.dispose());
        rod.add(fechar);
        root.add(rod, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setVisible(true);
    }

    /** Diálogo de formulário com Cancelar/Salvar. onSave devolve true para fechar. */
    private void formDialog(String titulo, String subtitulo, int w, int h, JComponent form,
                            String textoSalvar, Predicate<JDialog> onSave) {
        JDialog d = new JDialog(this, titulo, true);
        d.setSize(w, h);
        d.setLocationRelativeTo(this);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(cabecalho(titulo, subtitulo), BorderLayout.NORTH);

        JPanel card = new RoundPanel(Theme.CARD);
        card.setLayout(new BorderLayout());
        card.add(form, BorderLayout.NORTH);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(18, 22, 0, 22));
        wrap.add(card, BorderLayout.NORTH);
        root.add(wrap, BorderLayout.CENTER);

        JPanel rod = rodapeBotoes();
        ModernButton cancelar = new ModernButton("Cancelar", ModernButton.Kind.SECONDARY);
        ModernButton salvar = new ModernButton(textoSalvar, ModernButton.Kind.PRIMARY);
        cancelar.addActionListener(e -> d.dispose());
        salvar.addActionListener(e -> {
            try {
                if (onSave.test(d)) d.dispose();
            } catch (Exception ex) {
                erro(d, ex.getMessage() != null ? ex.getMessage() : ex.toString());
            }
        });
        rod.add(cancelar);
        rod.add(salvar);
        root.add(rod, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.getRootPane().setDefaultButton(salvar);
        d.setVisible(true);
    }

    // =====================================================================
    // TELA PRINCIPAL (MENU)
    // =====================================================================
    public AlimentaBemApp() {
        setTitle("AlimentaBem — Sistema de Doação de Alimentos");
        setSize(1120, 740);
        setMinimumSize(new Dimension(1000, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Image icone = Marca.escalada(64);
        if (icone != null) setIconImage(icone);

        JPanel root = new FundoPanel();
        root.setLayout(new BorderLayout());

        // cabeçalho com logo, título e data
        GradientPanel header = new GradientPanel();
        header.setLayout(new BorderLayout(24, 0));
        header.setBorder(new EmptyBorder(26, 34, 46, 34));
        header.add(new LogoChip(92), BorderLayout.WEST);

        JLabel selo = new JLabel("PAINEL DE GESTÃO  •  BANCO DE ALIMENTOS");
        selo.setFont(Theme.bodyBold(11));
        selo.setForeground(Theme.ACCENT);
        JLabel logo = new JLabel("AlimentaBem");
        logo.setFont(Theme.title(42));
        logo.setForeground(Color.WHITE);
        JLabel lema = new JLabel("Doação, estoque e distribuição de alimentos — sem desperdício");
        lema.setFont(Theme.body(14));
        lema.setForeground(new Color(0xBFF5EE));
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        for (JLabel l : new JLabel[]{selo, logo, lema}) l.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(Box.createVerticalGlue());
        textos.add(selo);
        textos.add(Box.createVerticalStrut(2));
        textos.add(logo);
        textos.add(Box.createVerticalStrut(2));
        textos.add(lema);
        textos.add(Box.createVerticalGlue());
        header.add(textos, BorderLayout.CENTER);

        String dia = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE", PT_BR));
        dia = Character.toUpperCase(dia.charAt(0)) + dia.substring(1);
        JPanel dataWrap = new JPanel(new GridBagLayout());
        dataWrap.setOpaque(false);
        dataWrap.add(new PillInfo(dia, LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", PT_BR))));
        header.add(dataWrap, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // corpo
        JPanel corpo = new JPanel(new BorderLayout(0, 20));
        corpo.setOpaque(false);
        corpo.setBorder(new EmptyBorder(22, 34, 14, 34));

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.add(statCard("Doadores cadastrados", statDoadores, Theme.PRIMARY_DARK, "doador"));
        stats.add(statCard("Alimentos cadastrados", statAlimentos, Theme.PRIMARY_DARK, "alimento"));
        stats.add(statCard("Lotes válidos em estoque", statLotes, Theme.PRIMARY_DARK, "estoque"));
        stats.add(statCard("Lotes vencendo em 7 dias", statVencer, Theme.WARN, "relogio"));
        corpo.add(stats, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 4, 16, 16));
        grid.setOpaque(false);
        grid.add(new MenuCard("doador", "Doadores", "Pessoas físicas e empresas doadoras", MenuCard.CLARO, this::abrirTelaDoadores));
        grid.add(new MenuCard("alimento", "Alimentos", "Catálogo, categorias e valor nutricional (USDA)", MenuCard.CLARO, this::abrirTelaAlimentos));
        grid.add(new MenuCard("instituicao", "Instituições", "Beneficiárias com endereço via CEP", MenuCard.CLARO, this::abrirTelaInstituicoes));
        grid.add(new MenuCard("estoque", "Estoque", "Lotes ordenados por validade (FIFO)", MenuCard.CLARO, this::abrirTelaEstoque));
        grid.add(new MenuCard("doacao", "Doações", "Registrar e consultar doações recebidas", MenuCard.CLARO, this::abrirTelaDoacoes));
        grid.add(new MenuCard("distribuicao", "Distribuições", "Saída de estoque para instituições, com sugestão FIFO", MenuCard.GRADIENTE, this::abrirTelaDistribuicoes));
        grid.add(new MenuCard("relatorio", "Relatórios", "CSV, impacto nutricional e vencimentos", MenuCard.ESCURO, this::abrirTelaRelatorios));
        grid.add(new MenuCard("sair", "Sair", "Encerrar o sistema", MenuCard.PERIGO, () -> {
            if (confirmar(this, "Encerrar o AlimentaBem?\nOs dados ficam salvos para a próxima vez.")) System.exit(0);
        }));
        JLabel rodape = rotuloMuted("AlimentaBem  •  Projeto IP2  •  UFRPE  •  2026.2");
        if (fachada.getArquivoDados() != null) {
            rodape.setToolTipText("Dados salvos em " + fachada.getArquivoDados().toAbsolutePath());
        }
        rodape.setHorizontalAlignment(SwingConstants.CENTER);
        corpo.add(rodape, BorderLayout.SOUTH);
        corpo.add(grid, BorderLayout.CENTER);

        root.add(corpo, BorderLayout.CENTER);
        setContentPane(root);
        refreshStats();
    }

    private JComponent statCard(String legenda, JLabel valor, Color cor, String icone) {
        valor.setFont(Theme.title(32));
        valor.setForeground(cor);
        RoundPanel p = new RoundPanel(Theme.CARD, true);
        p.setLayout(new BorderLayout(10, 0));
        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        textos.add(valor, BorderLayout.CENTER);
        textos.add(rotuloMuted(legenda), BorderLayout.SOUTH);
        p.add(textos, BorderLayout.CENTER);
        JPanel ic = new JPanel(new GridBagLayout());
        ic.setOpaque(false);
        ic.add(new Badge(icone, new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 30), cor));
        p.add(ic, BorderLayout.EAST);
        return p;
    }

    /** Anima o número de 0 até o valor atual (efeito de contagem). */
    private void contar(JLabel label, long alvo) {
        long de;
        try {
            de = Long.parseLong(label.getText());
        } catch (NumberFormatException e) {
            de = 0;
        }
        if (de == alvo) {
            label.setText(String.valueOf(alvo));
            return;
        }
        final long inicio = de;
        final int[] quadro = {0};
        final int total = 18;
        javax.swing.Timer timer = new javax.swing.Timer(18, null);
        timer.addActionListener(ev -> {
            quadro[0]++;
            double k = 1 - Math.pow(1 - quadro[0] / (double) total, 3);
            label.setText(String.valueOf(Math.round(inicio + (alvo - inicio) * k)));
            if (quadro[0] >= total) {
                label.setText(String.valueOf(alvo));
                timer.stop();
            }
        });
        timer.start();
    }

    private void refreshStats() {
        contar(statDoadores, fachada.listarDoadores().size());
        contar(statAlimentos, fachada.listarAlimentos().size());
        LocalDate hoje = LocalDate.now();
        long validos = fachada.listarEstoque().stream()
                .filter(l -> l.getQuantidadeDisponivel() > 0 && !l.getDataValidade().isBefore(hoje)).count();
        long vencer = lotesAVencer(7).size();
        contar(statLotes, validos);
        contar(statVencer, vencer);
        statVencer.setForeground(vencer > 0 ? Theme.WARN : Theme.PRIMARY_DARK);
    }

    // =====================================================================
    // 1. DOADORES  (REQ01)
    // =====================================================================
    // REQ01: tela de doadores (Pessoa Física e Empresa)
    private void abrirTelaDoadores() {
        ListScreen<Doador> tela = new ListScreen<>("Doadores", "Pessoas físicas e empresas que doam alimentos",
                new String[]{"Nome", "Tipo", "Telefone", "E-mail", "CPF / CNPJ"},
                () -> new ArrayList<>(fachada.listarDoadores()),
                d -> new Object[]{d.getNome(), d instanceof Empresa ? "Empresa" : "Pessoa Física",
                        d.getTelefone(), d.getEmail(), d.getDocumento()},
                780, 520);
        tela.addAction("Novo Doador", ModernButton.Kind.PRIMARY, this::abrirCadastroDoador);
        tela.addAction("Excluir", ModernButton.Kind.DANGER, () -> {
            Doador sel = tela.selecionado();
            if (sel == null) { aviso(tela.dialog, "Selecione um doador na tabela."); return; }
            boolean temDoacoes = fachada.listarDoacoes().stream()
                    .anyMatch(dc -> Objects.equals(dc.getDoador().getDocumento(), sel.getDocumento()));
            if (temDoacoes) { // evita doações "órfãs" (o controlador do projeto não verifica isso)
                erro(tela.dialog, "Este doador já possui doações registradas e não pode ser excluído.");
                return;
            }
            if (confirmar(tela.dialog, "Excluir o doador \"" + sel.getNome() + "\"?")) {
                fachada.removerDoador(sel.getDocumento());
            }
        });
        tela.show();
    }

    // REQ01: cadastro de doador: Pessoa Física (CPF) ou Empresa (CNPJ + razão social)
    private void abrirCadastroDoador() {
        JTextField nome = campo(), tel = campo(), email = campo(), doc = campo(), razao = campo();
        JComboBox<String> tipo = combo(Arrays.asList("Pessoa Física (CPF)", "Empresa (CNPJ)"), s -> s);
        habilitarCampo(razao, false);
        tipo.addActionListener(e -> habilitarCampo(razao, tipo.getSelectedIndex() == 1));

        Form f = new Form();
        f.linha("Nome *", nome);
        f.linha("Telefone", tel);
        f.linha("E-mail", email);
        f.linha("Tipo *", tipo);
        f.linha("CPF / CNPJ *", doc);
        f.linha("Razão social (empresa)", razao);

        formDialog("Novo Doador", "Cadastre uma pessoa física ou uma empresa", 520, 520, f, "Salvar", d -> {
            boolean empresa = tipo.getSelectedIndex() == 1;
            String numero = soDigitos(doc.getText());
            if (nome.getText().isBlank()) { aviso(d, "Informe o nome do doador."); return false; }
            if (!email.getText().isBlank() && !email.getText().contains("@")) {
                aviso(d, "Informe um e-mail válido."); return false;
            }
            if (numero.length() != (empresa ? 14 : 11)) {
                aviso(d, empresa ? "O CNPJ deve ter 14 dígitos." : "O CPF deve ter 11 dígitos."); return false;
            }
            if (empresa && razao.getText().isBlank()) { aviso(d, "Informe a razão social da empresa."); return false; }
            if (empresa) {
                fachada.cadastrarDoador(new Empresa(nome.getText().trim(), tel.getText().trim(),
                        email.getText().trim(), numero, razao.getText().trim()));
            } else {
                fachada.cadastrarDoador(new PessoaFisica(nome.getText().trim(), tel.getText().trim(),
                        email.getText().trim(), numero));
            }
            sucesso(d, "Doador cadastrado com sucesso!");
            return true;
        });
    }

    // =====================================================================
    // 2. ALIMENTOS  (REQ03, REQ05)
    // =====================================================================
    // REQ03: tela de alimentos
    // REQ05: botão "Valor Nutricional (USDA)"
    private void abrirTelaAlimentos() {
        ListScreen<Alimento> tela = new ListScreen<>("Alimentos", "Catálogo de alimentos e composição nutricional",
                new String[]{"Nome", "Categoria", "Unidade", "Calorias / 100g", "Proteínas / 100g", "Fonte"},
                () -> new ArrayList<>(fachada.listarAlimentos()),
                a -> {
                    ValorNutricional v = a.getValorNutricional();
                    return new Object[]{a.getNome(), rotulo(a.getCategoria()), unidadeComPeso(a),
                            v != null ? nf(v.getCaloriasPor100g()) + " kcal" : "Não informado",
                            v != null ? nf(v.getProteinasPor100gEmGramas()) + " g" : "—",
                            v != null ? v.getFonte() : "—"};
                }, 900, 540);
        tela.addAction("Novo Alimento", ModernButton.Kind.PRIMARY, () -> abrirFormAlimento(null));
        tela.addAction("Excluir", ModernButton.Kind.DANGER, () -> {
            Alimento sel = tela.selecionado();
            if (sel == null) { aviso(tela.dialog, "Selecione um alimento na tabela."); return; }
            if (confirmar(tela.dialog, "Excluir o alimento \"" + sel.getNome() + "\"?")) {
                fachada.removerAlimento(sel.getNome()); // bloqueado se já existir lote desse alimento
            }
        });
        tela.addAction("Editar", ModernButton.Kind.SECONDARY, () -> {
            Alimento sel = tela.selecionado();
            if (sel == null) aviso(tela.dialog, "Selecione um alimento na tabela.");
            else abrirFormAlimento(sel);
        });
        tela.addAction("Valor Nutricional (USDA)", ModernButton.Kind.PRIMARY, () -> {
            Alimento sel = tela.selecionado();
            if (sel == null) aviso(tela.dialog, "Selecione um alimento na tabela.");
            else abrirValorNutricional(sel);
        });
        tela.show();
    }

    /** Cadastro (existente == null) ou alteração de alimento. */
    // REQ03: cadastro/alteração de alimento: nome, categoria e unidade de medida
    private void abrirFormAlimento(Alimento existente) {
        JTextField nome = campo(existente != null ? existente.getNome() : "");
        JComboBox<CategoriaAlimento> cat = combo(Arrays.asList(CategoriaAlimento.values()), AlimentaBemApp::rotulo);
        JComboBox<UnidadeMedida> un = combo(Arrays.asList(UnidadeMedida.values()), AlimentaBemApp::rotulo);
        // REQ06: peso médio de 1 unidade (g), usado no cálculo nutricional de alimentos contados em unidades
        JTextField peso = campoDecimal(existente != null && existente.getPesoPorUnidadeGramas() != null
                ? qf(existente.getPesoPorUnidadeGramas()) : "");
        if (existente != null) {
            cat.setSelectedItem(existente.getCategoria());
            un.setSelectedItem(existente.getUnidadeMedida());
        }
        Runnable ajustarPeso = () -> habilitarCampo(peso, un.getSelectedItem() == UnidadeMedida.UNIDADE);
        un.addActionListener(e -> ajustarPeso.run());
        ajustarPeso.run();
        Form f = new Form();
        f.linha("Nome do alimento *", nome);
        f.linha("Categoria *", cat);
        f.linha("Unidade de medida *", un);
        f.linha("Peso de 1 unidade (g) *", peso);
        f.linha("", rotuloMuted("Só para \"Unidade\" (ex.: 1 ovo ≈ 50 g)."));

        String titulo = existente == null ? "Novo Alimento" : "Alterar Alimento";
        formDialog(titulo, "Defina nome, categoria e unidade de medida", 540, 480, f, "Salvar", d -> {
            if (nome.getText().isBlank()) { aviso(d, "Informe o nome do alimento."); return false; }
            CategoriaAlimento c = (CategoriaAlimento) cat.getSelectedItem();
            UnidadeMedida u = (UnidadeMedida) un.getSelectedItem();
            Double g = null;
            if (u == UnidadeMedida.UNIDADE) {
                g = parseNum(peso.getText());
                if (g == null || g <= 0) {
                    aviso(d, "Informe o peso médio de 1 unidade em gramas (número maior que zero).");
                    return false;
                }
            }
            if (existente == null) {
                fachada.cadastrarAlimento(nome.getText().trim(), c, u, g);
                sucesso(d, "Alimento cadastrado!");
            } else {
                fachada.atualizarAlimento(existente.getNome(), nome.getText().trim(), c, u, g);
                sucesso(d, "Alimento atualizado!");
            }
            return true;
        });
    }

    // REQ05: busca calorias/proteínas na API USDA FoodData Central (ou preenchimento manual)
    private void abrirValorNutricional(Alimento alimento) {
        ValorNutricional atual = alimento.getValorNutricional();
        String chaveEnv = System.getenv("USDA_API_KEY");
        JTextField termo = campo(alimento.getNome());
        JTextField chave = campo(chaveEnv != null && !chaveEnv.isBlank() ? chaveEnv : "DEMO_KEY");
        JTextField cal = campoDecimal(atual != null ? String.format(Locale.US, "%.1f", atual.getCaloriasPor100g()) : "");
        JTextField prot = campoDecimal(atual != null ? String.format(Locale.US, "%.1f", atual.getProteinasPor100gEmGramas()) : "");
        JTextField fonte = campo(atual != null ? atual.getFonte() : "USDA FoodData Central");
        JLabel status = rotuloMuted("Digite o nome em inglês (ex.: rice, milk, apple) e clique em Buscar.");

        ModernButton buscar = new ModernButton("Buscar na USDA", ModernButton.Kind.PRIMARY);
        buscar.addActionListener(e -> {
            buscar.setEnabled(false);
            status.setForeground(Theme.MUTED);
            status.setText("Consultando USDA FoodData Central...");
            async(() -> consultarUsda(termo.getText().trim(), chave.getText().trim()),
                    r -> {
                        buscar.setEnabled(true);
                        if (r == null) {
                            status.setForeground(Theme.DANGER);
                            status.setText("Nenhum resultado encontrado para esse termo.");
                            return;
                        }
                        if (r.kcal != null) cal.setText(String.format(Locale.US, "%.1f", r.kcal));
                        if (r.proteina != null) prot.setText(String.format(Locale.US, "%.1f", r.proteina));
                        status.setForeground(Theme.OK);
                        status.setText("Encontrado: " + r.descricao);
                    },
                    ex -> {
                        buscar.setEnabled(true);
                        status.setForeground(Theme.DANGER);
                        status.setText(ex.getMessage());
                    });
        });

        JPanel linhaBusca = new JPanel(new BorderLayout(8, 0));
        linhaBusca.setOpaque(false);
        linhaBusca.add(termo, BorderLayout.CENTER);
        linhaBusca.add(buscar, BorderLayout.EAST);

        Form f = new Form();
        f.linha("Termo (inglês)", linhaBusca);
        f.linha("Chave da API", chave);
        f.linha("Calorias / 100g *", cal);
        f.linha("Proteínas / 100g (g) *", prot);
        f.linha("Fonte dos dados", fonte);
        f.linha("", status);

        formDialog("Valor Nutricional — " + alimento.getNome(), "Consulta à USDA FoodData Central ou preenchimento manual",
                640, 520, f, "Salvar", d -> {
                    Double c = parseNum(cal.getText());
                    Double p = parseNum(prot.getText());
                    if (c == null || p == null || c < 0 || p < 0) {
                        aviso(d, "Informe valores numéricos válidos (maiores ou iguais a zero)."); return false;
                    }
                    fachada.definirValorNutricional(alimento, new ValorNutricional(c, p, fonte.getText().trim()));
                    sucesso(d, "Valor nutricional registrado!");
                    return true;
                });
    }

    // =====================================================================
    // 3. INSTITUIÇÕES  (REQ02, REQ14, REQ20)
    // =====================================================================
    // REQ02: tela de instituições beneficiárias
    // REQ20: botão Excluir (bloqueado se houver distribuições)
    private void abrirTelaInstituicoes() {
        YearMonth mes = YearMonth.now();
        ListScreen<InstituicaoBeneficiaria> tela = new ListScreen<>("Instituições Beneficiárias",
                "Entidades que recebem as distribuições",
                new String[]{"Nome", "CEP", "Endereço", "Capacidade mensal", "Atendidas no mês"},
                () -> new ArrayList<>(fachada.listarInstituicoes()),
                i -> new Object[]{i.getNome(), formatarCep(i.getCep()), enderecoTexto(i.getEndereco()),
                        i.getCapacidadeMensal() + " famílias",
                        familiasNoMes(i, mes) + " / " + i.getCapacidadeMensal() + " famílias"},
                960, 540);
        tela.addAction("Nova Instituição", ModernButton.Kind.PRIMARY, this::abrirCadastroInstituicao);
        tela.addAction("Excluir", ModernButton.Kind.DANGER, () -> {
            InstituicaoBeneficiaria sel = tela.selecionado();
            if (sel == null) { aviso(tela.dialog, "Selecione uma instituição na tabela."); return; }
            if (confirmar(tela.dialog, "Excluir a instituição \"" + sel.getNome() + "\"?")) {
                // REQ20: o serviço impede excluir instituição com distribuições registradas
                fachada.removerInstituicao(sel.getNome()); // o serviço bloqueia se houver distribuições
            }
        });
        tela.show();
    }

    // REQ02: cadastro de instituição: nome, CEP (endereço automático via BrasilAPI) e capacidade mensal
    // REQ14: CEP inválido ou sem retorno da API bloqueia o cadastro
    private void abrirCadastroInstituicao() {
        JTextField nome = campo(), cep = campo(), cap = campoInteiro();
        JTextField rua = campoSomenteLeitura(), bairro = campoSomenteLeitura(),
                cidade = campoSomenteLeitura(), uf = campoSomenteLeitura();
        JLabel status = rotuloMuted("Informe o CEP e clique em Buscar para completar o endereço.");
        String[] cepValidado = {null};

        ModernButton buscar = new ModernButton("Buscar CEP", ModernButton.Kind.PRIMARY);
        Consumer<String[]> preencher = r -> {
            rua.setText(r[0]); bairro.setText(r[1]); cidade.setText(r[2]); uf.setText(r[3]);
        };
        buscar.addActionListener(e -> {
            String limpo = soDigitos(cep.getText());
            if (limpo.length() != 8) {
                status.setForeground(Theme.DANGER);
                status.setText("O CEP deve ter 8 dígitos.");
                return;
            }
            buscar.setEnabled(false);
            status.setForeground(Theme.MUTED);
            status.setText("Consultando BrasilAPI...");
            async(() -> consultarCep(limpo),
                    r -> {
                        buscar.setEnabled(true);
                        preencher.accept(r);
                        cepValidado[0] = limpo;
                        status.setForeground(Theme.OK);
                        status.setText("Endereço localizado.");
                    },
                    ex -> {
                        buscar.setEnabled(true);
                        cepValidado[0] = null;
                        preencher.accept(new String[]{"", "", "", ""});
                        status.setForeground(Theme.DANGER);
                        status.setText(ex.getMessage());
                    });
        });

        JPanel linhaCep = new JPanel(new BorderLayout(8, 0));
        linhaCep.setOpaque(false);
        linhaCep.add(cep, BorderLayout.CENTER);
        linhaCep.add(buscar, BorderLayout.EAST);

        Form f = new Form();
        f.linha("Nome *", nome);
        f.linha("CEP *", linhaCep);
        f.linha("", status);
        f.linha("Rua", rua);
        f.linha("Bairro", bairro);
        f.linha("Cidade", cidade);
        f.linha("Estado", uf);
        f.linha("Capacidade mensal (famílias) *", cap);

        formDialog("Nova Instituição", "O endereço é completado automaticamente pelo CEP", 640, 620, f, "Salvar", d -> {
            if (nome.getText().isBlank()) { aviso(d, "Informe o nome da instituição."); return false; }
            Integer capacidade = parseInt(cap.getText());
            if (capacidade == null || capacidade <= 0) {
                aviso(d, "Informe uma capacidade mensal válida (número inteiro maior que zero)."); return false;
            }
            String limpo = soDigitos(cep.getText());
            if (limpo.length() != 8) { aviso(d, "O CEP deve ter 8 dígitos."); return false; }

            d.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                fachada.cadastrarInstituicao(nome.getText().trim(), limpo, capacidade);
            } finally {
                d.setCursor(Cursor.getDefaultCursor());
            }
            sucesso(d, "Instituição registrada!");
            return true;
        });
    }

    // =====================================================================
    // 4. ESTOQUE  (REQ07)
    // =====================================================================
    // REQ07: estoque por lote, ordenado por data de validade
    // REQ13: lotes vencidos aparecem marcados como bloqueados
    private void abrirTelaEstoque() {
        ListScreen<LoteEstoque> tela = new ListScreen<>("Estoque por Lote",
                "Ordenado por data de validade — lotes mais próximos do vencimento primeiro",
                new String[]{"Lote", "Alimento", "Categoria", "Validade", "Disponível", "Situação"},
                this::lotesOrdenados,
                l -> new Object[]{idCurto(l), l.getAlimento().getNome(), rotulo(l.getAlimento().getCategoria()),
                        DF.format(l.getDataValidade()),
                        qf(l.getQuantidadeDisponivel()) + " " + unidadeCurta(l.getAlimento()),
                        statusLote(l)},
                940, 580);
        tela.aoRecarregar(() -> tela.setExtra(chipsEstoque()));
        tela.show();
    }

    private JComponent chipsEstoque() {
        long validos = 0, vencendo = 0, vencidos = 0;
        for (LoteEstoque l : fachada.listarEstoque()) {
            if (l.getQuantidadeDisponivel() <= 0) continue;
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), l.getDataValidade());
            if (dias < 0) vencidos++; else if (dias <= 7) vencendo++; else validos++;
        }
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.add(new Chip(validos + " lote(s) válidos", new Color(0xD3F6EF), new Color(0x00675A)));
        p.add(new Chip(vencendo + " vencendo em 7 dias", new Color(0xFEF3C7), new Color(0x92400E)));
        p.add(new Chip(vencidos + " vencido(s) — bloqueados", new Color(0xFEE2E2), new Color(0xB91C1C)));
        return p;
    }

    // =====================================================================
    // 5. DOAÇÕES  (REQ04, REQ06, REQ15, REQ19)
    // =====================================================================
    // REQ04: tela de doações
    // REQ06: calorias e proteínas totais de cada doação
    private void abrirTelaDoacoes() {
        ListScreen<Doacao> tela = new ListScreen<>("Doações", "Doações recebidas (duplo clique para ver os detalhes)",
                new String[]{"Data", "Doador", "Itens", "Calorias totais", "Proteínas totais"},
                () -> {
                    List<Doacao> l = new ArrayList<>(fachada.listarDoacoes());
                    Collections.reverse(l);
                    return l;
                },
                d -> {
                    double[] n = nutriDoacao(d);
                    return new Object[]{DF.format(d.getDataDoacao()), d.getDoador().getNome(),
                            d.getItens().size(), nf(n[0]) + " kcal", nf(n[1]) + " g"};
                }, 860, 540);
        tela.addAction("Nova Doação", ModernButton.Kind.PRIMARY, this::abrirCadastroDoacao);
        tela.addAction("Detalhes", ModernButton.Kind.SECONDARY, () -> {
            Doacao sel = tela.selecionado();
            if (sel == null) aviso(tela.dialog, "Selecione uma doação na tabela.");
            else abrirDetalhesDoacao(sel);
        });
        tela.aoDuploClique(() -> {
            Doacao sel = tela.selecionado();
            if (sel != null) abrirDetalhesDoacao(sel);
        });
        tela.show();
    }

    private void abrirDetalhesDoacao(Doacao doacao) {
        List<Object[]> linhas = new ArrayList<>();
        double kcal = 0, prot = 0;
        int semNutri = 0;
        for (ItemDoacao it : doacao.getItens()) {
            double[] n = nutri(it.getAlimento(), it.getQuantidade());
            kcal += n[0];
            prot += n[1];
            if (semDadosNutricionais(it.getAlimento())) semNutri++;
            linhas.add(new Object[]{it.getAlimento().getNome(), rotulo(it.getAlimento().getCategoria()),
                    qf(it.getQuantidade()) + " " + unidadeCurta(it.getAlimento()),
                    DF.format(it.getDataValidade()), nf(n[0]) + " kcal", nf(n[1]) + " g"});
        }
        String rodape = "Total da doação: " + nf(kcal) + " kcal  •  " + nf(prot) + " g de proteína"
                + (semNutri > 0 ? "<br><span style='color:#B45309'>" + semNutri
                + " item(ns) sem valor nutricional (ou sem peso por unidade) não entraram no cálculo.</span>" : "");
        dialogoTabela("Detalhes da Doação",
                doacao.getDoador().getNome() + "  •  " + DF.format(doacao.getDataDoacao()),
                new String[]{"Alimento", "Categoria", "Quantidade", "Validade", "Calorias", "Proteínas"},
                linhas, rodape, 820, 480);
    }

    // REQ04: registro de doação vinculada a um doador, com itens (alimento, quantidade e validade)
    // REQ15: não permite finalizar doação sem itens
    private void abrirCadastroDoacao() {
        if (fachada.listarDoadores().isEmpty() || fachada.listarAlimentos().isEmpty()) {
            aviso(this, "Cadastre ao menos um doador e um alimento antes de criar uma doação!");
            return;
        }
        JDialog d = new JDialog(this, "Nova Doação", true);
        d.setSize(820, 620);
        d.setLocationRelativeTo(this);

        JComboBox<Doador> cbDoador = combo(fachada.listarDoadores(), x -> x.getNome() + "  —  " + x.getDocumento());
        List<ItemDoacao> itens = new ArrayList<>();
        DefaultTableModel m = modeloTabela("Alimento", "Categoria", "Quantidade", "Validade", "Calorias", "Proteínas");
        JTable t = criarTabela(m);
        // REQ06: total de calorias e proteínas da doação, calculado ao vivo
        JLabel totais = new JLabel();
        totais.setFont(Theme.bodyBold(14));
        totais.setForeground(Theme.PRIMARY_DARK);

        Runnable atualizar = () -> {
            m.setRowCount(0);
            double k = 0, p = 0;
            for (ItemDoacao it : itens) {
                double[] n = nutri(it.getAlimento(), it.getQuantidade());
                k += n[0];
                p += n[1];
                m.addRow(new Object[]{it.getAlimento().getNome(), rotulo(it.getAlimento().getCategoria()),
                        qf(it.getQuantidade()) + " " + unidadeCurta(it.getAlimento()),
                        DF.format(it.getDataValidade()), nf(n[0]) + " kcal", nf(n[1]) + " g"});
            }
            totais.setText("Total da doação:  " + nf(k) + " kcal   •   " + nf(p) + " g de proteína   •   "
                    + itens.size() + " item(ns)");
        };
        atualizar.run();

        ModernButton add = new ModernButton("Adicionar Item", ModernButton.Kind.PRIMARY);
        add.addActionListener(e -> abrirAdicionarItemDoacao(itens, atualizar));
        ModernButton remover = new ModernButton("Remover Item", ModernButton.Kind.SECONDARY);
        remover.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r < 0) { aviso(d, "Selecione um item na tabela."); return; }
            itens.remove(t.convertRowIndexToModel(r));
            atualizar.run();
        });
        ModernButton cancelar = new ModernButton("Cancelar", ModernButton.Kind.SECONDARY);
        cancelar.addActionListener(e -> d.dispose());
        ModernButton finalizar = new ModernButton("Finalizar e Registrar", ModernButton.Kind.PRIMARY);
        finalizar.addActionListener(e -> {
            if (itens.isEmpty()) { // REQ15
                aviso(d, "Uma doação precisa ter pelo menos 1 item.");
                return;
            }
            LocalDate hoje = LocalDate.now();
            for (ItemDoacao it : itens) { // REQ19
                if (it.getDataValidade().isBefore(hoje)) {
                    erro(d, "O item \"" + it.getAlimento().getNome() + "\" está vencido e não pode ser recebido.");
                    return;
                }
            }
            try {
                Doacao nova = new Doacao((Doador) cbDoador.getSelectedItem(), hoje);
                for (ItemDoacao it : itens) nova.adicionarItem(it);
                fachada.registrarDoacao(nova);
                sucesso(d, "Doação registrada e lotes adicionados ao estoque!");
                d.dispose();
            } catch (Exception ex) {
                erro(d, ex.getMessage() != null ? ex.getMessage() : ex.toString());
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(cabecalho("Nova Doação", "Escolha o doador e adicione os itens recebidos"), BorderLayout.NORTH);

        JPanel corpo = new JPanel(new BorderLayout(0, 12));
        corpo.setOpaque(false);
        corpo.setBorder(new EmptyBorder(16, 22, 0, 22));
        JPanel topo = new JPanel(new BorderLayout(10, 0));
        topo.setOpaque(false);
        topo.add(rotuloMuted("Doador"), BorderLayout.WEST);
        topo.add(cbDoador, BorderLayout.CENTER);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(add);
        acoes.add(remover);
        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setOpaque(false);
        norte.add(topo, BorderLayout.NORTH);
        norte.add(acoes, BorderLayout.SOUTH);
        corpo.add(norte, BorderLayout.NORTH);
        corpo.add(rolagem(t), BorderLayout.CENTER);
        corpo.add(totais, BorderLayout.SOUTH);
        root.add(corpo, BorderLayout.CENTER);

        JPanel rod = rodapeBotoes();
        rod.add(cancelar);
        rod.add(finalizar);
        root.add(rod, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setVisible(true);
    }

    // REQ04: item de doação: alimento, quantidade e data de validade
    // REQ19: bloqueia item com validade já vencida
    private void abrirAdicionarItemDoacao(List<ItemDoacao> itens, Runnable aposAdicionar) {
        JComboBox<Alimento> cb = combo(fachada.listarAlimentos(), a -> a.getNome() + "  (" + rotulo(a.getCategoria()) + ")");
        JTextField qtd = campoDecimal("");
        JTextField validade = campo(LocalDate.now().plusMonths(3).format(DF));
        Form f = new Form();
        f.linha("Alimento *", cb);
        f.linha("Quantidade *", qtd);
        f.linha("Validade * (dd/mm/aaaa)", validade);

        formDialog("Adicionar Item de Doação", "Informe a quantidade e a validade do lote", 520, 380, f, "Adicionar", d -> {
            Double q = parseNum(qtd.getText());
            if (q == null || q <= 0) { aviso(d, "Informe uma quantidade maior que zero."); return false; }
            LocalDate val;
            try {
                val = parseData(validade.getText());
            } catch (DateTimeParseException ex) {
                aviso(d, "Data inválida. Use o formato dd/mm/aaaa."); return false;
            }
            if (val.isBefore(LocalDate.now())) { // REQ19
                erro(d, "A validade informada já está vencida. Itens vencidos não podem ser recebidos.");
                return false;
            }
            itens.add(new ItemDoacao((Alimento) cb.getSelectedItem(), q, val));
            aposAdicionar.run();
            return true;
        });
    }

    // =====================================================================
    // 6. DISTRIBUIÇÕES  (REQ08, REQ09, REQ13, REQ16, REQ17, REQ18)
    // =====================================================================
    // REQ08: tela de distribuições
    private void abrirTelaDistribuicoes() {
        ListScreen<Distribuicao> tela = new ListScreen<>("Distribuições",
                "Saídas de estoque para instituições (duplo clique para ver os detalhes)",
                new String[]{"Data", "Instituição", "Famílias atendidas", "Itens", "Calorias", "Proteínas"},
                () -> {
                    List<Distribuicao> l = new ArrayList<>(fachada.listarTodasDistribuicoes());
                    Collections.reverse(l);
                    return l;
                },
                dist -> {
                    double[] n = nutriDistribuicao(dist);
                    return new Object[]{DF.format(dist.getDataDistribuicao()), dist.getInstituicao().getNome(),
                            dist.getFamiliasAtendidas(), dist.getItens().size(),
                            nf(n[0]) + " kcal", nf(n[1]) + " g"};
                }, 920, 540);
        tela.addAction("Nova Distribuição", ModernButton.Kind.PRIMARY, this::abrirCadastroDistribuicao);
        tela.addAction("Detalhes", ModernButton.Kind.SECONDARY, () -> {
            Distribuicao sel = tela.selecionado();
            if (sel == null) aviso(tela.dialog, "Selecione uma distribuição na tabela.");
            else abrirDetalhesDistribuicao(sel);
        });
        tela.aoDuploClique(() -> {
            Distribuicao sel = tela.selecionado();
            if (sel != null) abrirDetalhesDistribuicao(sel);
        });
        tela.show();
    }

    private void abrirDetalhesDistribuicao(Distribuicao dist) {
        List<Object[]> linhas = new ArrayList<>();
        for (ItemDistribuicao it : dist.getItens()) {
            Alimento a = it.getLote().getAlimento();
            double[] n = nutri(a, it.getQuantidadeRetirada());
            linhas.add(new Object[]{idCurto(it.getLote()), a.getNome(),
                    DF.format(it.getLote().getDataValidade()),
                    qf(it.getQuantidadeRetirada()) + " " + unidadeCurta(a),
                    nf(n[0]) + " kcal", nf(n[1]) + " g"});
        }
        double[] tot = nutriDistribuicao(dist);
        dialogoTabela("Detalhes da Distribuição",
                dist.getInstituicao().getNome() + "  •  " + DF.format(dist.getDataDistribuicao())
                        + "  •  " + dist.getFamiliasAtendidas() + " famílias",
                new String[]{"Lote", "Alimento", "Validade", "Retirado", "Calorias", "Proteínas"},
                linhas, "Impacto nutricional: " + nf(tot[0]) + " kcal  •  " + nf(tot[1]) + " g de proteína",
                820, 460);
    }

    // REQ08: distribuição para uma ou mais instituições com os itens retirados do estoque
    // REQ17: mostra a capacidade mensal restante de cada instituição
    private void abrirCadastroDistribuicao() {
        if (fachada.listarInstituicoes().isEmpty()) {
            aviso(this, "Cadastre ao menos uma instituição antes!");
            return;
        }
        if (alimentosComEstoqueValido().isEmpty()) {
            aviso(this, "Não há estoque válido (não vencido) disponível para distribuir.");
            return;
        }
        YearMonth mes = YearMonth.now();
        JDialog d = new JDialog(this, "Nova Distribuição", true);
        d.setSize(980, 660);
        d.setLocationRelativeTo(this);

        // --- instituições (seleção múltipla)
        DefaultListModel<InstituicaoBeneficiaria> lm = new DefaultListModel<>();
        fachada.listarInstituicoes().forEach(lm::addElement);
        JList<InstituicaoBeneficiaria> lista = new JList<>(lm);
        lista.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lista.setFixedCellHeight(66);
        lista.setSelectionBackground(new Color(0xC8F0EA));
        lista.setSelectionForeground(Theme.TEXT);
        CelulaInstituicao celula = new CelulaInstituicao();
        lista.setCellRenderer((l, inst, i, sel, foc) -> {
            int usado = familiasNoMes(inst, mes);
            int cap = inst.getCapacidadeMensal();
            int restam = cap - usado;
            celula.configurar(inst.getNome(),
                    restam <= 0 ? "Capacidade mensal atingida" : "Restam " + restam + " de " + cap + " famílias no mês",
                    restam <= 0, cap == 0 ? 0 : (double) usado / cap, sel);
            return celula;
        });

        JTextField familias = campoInteiro();

        // --- itens
        List<Object[]> itens = new ArrayList<>(); // {Alimento, Double qtdPorInstituicao}
        DefaultTableModel m = modeloTabela("Alimento", "Quantidade por instituição", "Disponível (válido)");
        JTable t = criarTabela(m);
        Runnable atualizar = () -> {
            m.setRowCount(0);
            for (Object[] it : itens) {
                Alimento a = (Alimento) it[0];
                m.addRow(new Object[]{a.getNome(), qf((Double) it[1]) + " " + unidadeCurta(a),
                        qf(disponivelValido(a)) + " " + unidadeCurta(a)});
            }
        };

        ModernButton add = new ModernButton("Adicionar Item", ModernButton.Kind.PRIMARY);
        add.addActionListener(e -> abrirAdicionarItemDistribuicao(itens, atualizar));
        ModernButton remover = new ModernButton("Remover Item", ModernButton.Kind.SECONDARY);
        remover.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r < 0) { aviso(d, "Selecione um item na tabela."); return; }
            itens.remove(t.convertRowIndexToModel(r));
            atualizar.run();
        });
        ModernButton fifo = new ModernButton("Sugestão de Lotes (FIFO)", ModernButton.Kind.SECONDARY);
        fifo.addActionListener(e -> abrirSugestaoFIFO(itens, Math.max(1, lista.getSelectedValuesList().size())));

        ModernButton cancelar = new ModernButton("Cancelar", ModernButton.Kind.SECONDARY);
        cancelar.addActionListener(e -> d.dispose());
        ModernButton registrar = new ModernButton("Registrar Distribuição", ModernButton.Kind.PRIMARY);
        registrar.addActionListener(e -> {
            List<InstituicaoBeneficiaria> sel = lista.getSelectedValuesList();
            if (sel.isEmpty()) { aviso(d, "Selecione ao menos uma instituição (Ctrl/Shift para várias)."); return; }
            Integer fam = parseInt(familias.getText());
            if (fam == null || fam <= 0) { aviso(d, "Informe o número de famílias atendidas por instituição."); return; }
            if (itens.isEmpty()) { aviso(d, "Adicione pelo menos 1 item a distribuir."); return; }
            try {
                String problema = validarERegistrarDistribuicao(sel, fam, itens);
                if (problema != null) { erro(d, problema); return; }
                sucesso(d, sel.size() == 1 ? "Distribuição concluída com sucesso!"
                        : sel.size() + " distribuições registradas com sucesso!");
                d.dispose();
            } catch (Exception ex) {
                erro(d, ex.getMessage() != null ? ex.getMessage() : ex.toString());
            }
        });

        // --- layout
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(cabecalho("Nova Distribuição",
                "Os lotes são sugeridos pela regra FIFO — vencimento mais próximo sai primeiro"), BorderLayout.NORTH);

        RoundPanel esquerda = new RoundPanel(Theme.CARD);
        esquerda.setLayout(new BorderLayout(0, 10));
        JLabel tit1 = new JLabel("Instituições beneficiárias");
        tit1.setFont(Theme.title(14));
        esquerda.add(tit1, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(lista);
        sp.setBorder(new LineBorder(Theme.BORDER));
        esquerda.add(sp, BorderLayout.CENTER);
        JPanel famPanel = new JPanel(new BorderLayout(0, 4));
        famPanel.setOpaque(false);
        famPanel.add(rotuloMuted("Famílias atendidas (por instituição)"), BorderLayout.NORTH);
        famPanel.add(familias, BorderLayout.CENTER);
        esquerda.add(famPanel, BorderLayout.SOUTH);
        esquerda.setPreferredSize(new Dimension(340, 0));

        RoundPanel direita = new RoundPanel(Theme.CARD);
        direita.setLayout(new BorderLayout(0, 10));
        JLabel tit2 = new JLabel("Itens retirados do estoque");
        tit2.setFont(Theme.title(14));
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(add);
        acoes.add(remover);
        acoes.add(fifo);
        JPanel norteDir = new JPanel(new BorderLayout(0, 8));
        norteDir.setOpaque(false);
        norteDir.add(tit2, BorderLayout.NORTH);
        norteDir.add(acoes, BorderLayout.SOUTH);
        direita.add(norteDir, BorderLayout.NORTH);
        direita.add(rolagem(t), BorderLayout.CENTER);
        JLabel dica = rotuloMuted("<html>A quantidade é por instituição selecionada. Lotes vencidos são ignorados automaticamente.</html>");
        direita.add(dica, BorderLayout.SOUTH);

        JPanel corpo = new JPanel(new BorderLayout(16, 0));
        corpo.setOpaque(false);
        corpo.setBorder(new EmptyBorder(16, 22, 0, 22));
        corpo.add(esquerda, BorderLayout.WEST);
        corpo.add(direita, BorderLayout.CENTER);
        root.add(corpo, BorderLayout.CENTER);

        JPanel rod = rodapeBotoes();
        rod.add(cancelar);
        rod.add(registrar);
        root.add(rod, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setVisible(true);
    }

    // REQ16: a quantidade não pode exceder o estoque válido disponível
    private void abrirAdicionarItemDistribuicao(List<Object[]> itens, Runnable apos) {
        List<Alimento> disponiveis = alimentosComEstoqueValido();
        JComboBox<Alimento> cb = combo(disponiveis,
                a -> a.getNome() + "  (disp.: " + qf(disponivelValido(a)) + " " + unidadeCurta(a) + ")");
        JTextField qtd = campoDecimal("");
        Form f = new Form();
        f.linha("Alimento *", cb);
        f.linha("Quantidade por instituição *", qtd);

        formDialog("Adicionar Item", "Somente alimentos com estoque válido aparecem aqui", 560, 320, f, "Adicionar", d -> {
            Double q = parseNum(qtd.getText());
            if (q == null || q <= 0) { aviso(d, "Informe uma quantidade maior que zero."); return false; }
            Alimento a = (Alimento) cb.getSelectedItem();
            double jaPedido = itens.stream().filter(x -> Objects.equals(x[0], a)).mapToDouble(x -> (Double) x[1]).sum();
            if (q + jaPedido > disponivelValido(a) + 1e-9) { // REQ16
                erro(d, "Quantidade maior que o estoque válido disponível ("
                        + qf(disponivelValido(a)) + " " + unidadeCurta(a) + ").");
                return false;
            }
            itens.add(new Object[]{a, q});
            apos.run();
            return true;
        });
    }

    /** Tela de Sugestão de Lotes (FIFO). */
    // REQ09: sugestão automática dos lotes mais próximos do vencimento
    // REQ18: lote de validade mais próxima é sempre o primeiro da lista
    private void abrirSugestaoFIFO(List<Object[]> itens, int nInstituicoes) {
        List<Object[]> linhas = new ArrayList<>();
        StringBuilder alerta = new StringBuilder();
        List<Alimento> alimentos = new ArrayList<>();
        Map<Alimento, Double> pedido = new LinkedHashMap<>();
        if (itens.isEmpty()) {
            alimentosComEstoqueValido().forEach(a -> pedido.put(a, 0.0));
        } else {
            for (Object[] it : itens) pedido.merge((Alimento) it[0], (Double) it[1] * nInstituicoes, Double::sum);
        }
        alimentos.addAll(pedido.keySet());

        for (Alimento a : alimentos) {
            double qtdTotal = pedido.get(a);
            IdentityHashMap<LoteEstoque, Double> reserva = new IdentityHashMap<>();
            List<Alocacao> plano = qtdTotal > 0 ? planejarFIFO(a, qtdTotal, reserva) : new ArrayList<>();
            Map<LoteEstoque, Double> sugerido = new IdentityHashMap<>();
            if (plano != null) plano.forEach(al -> sugerido.put(al.lote, al.qtd));
            else alerta.append("Estoque válido insuficiente de ").append(esc(a.getNome())).append(". ");

            int prioridade = 1;
            for (LoteEstoque l : lotesFIFO(a)) {
                if (l.getQuantidadeDisponivel() <= 0) continue;
                String situacao;
                String ordem;
                if (expirado(l)) {
                    situacao = "Bloqueado — lote vencido";
                    ordem = "—";
                } else {
                    ordem = prioridade++ + "º";
                    situacao = sugerido.containsKey(l)
                            ? "Sugerido: retirar " + qf(sugerido.get(l)) + " " + unidadeCurta(a)
                            : "Reserva";
                }
                linhas.add(new Object[]{ordem, a.getNome(), idCurto(l), DF.format(l.getDataValidade()),
                        qf(l.getQuantidadeDisponivel()) + " " + unidadeCurta(a), situacao});
            }
        }
        String rodape = alerta.length() > 0
                ? "<span style='color:#DC2626'>" + alerta + "</span>"
                : "O lote com validade mais próxima é sempre oferecido primeiro.";
        dialogoTabela("Sugestão de Lotes (FIFO)",
                itens.isEmpty() ? "Ordem de saída de todos os lotes válidos" : "Plano de saída para os itens informados",
                new String[]{"Prioridade", "Alimento", "Lote", "Validade", "Disponível", "Situação"},
                linhas, rodape, 900, 500);
    }

    /**
     * Valida e registra a distribuição (uma por instituição selecionada).
     * Retorna a mensagem de erro, ou null se tudo foi registrado.
     */
    // REQ08: registra uma distribuição por instituição selecionada
    // REQ13: lotes vencidos não entram no plano
    // REQ16: estoque insuficiente bloqueia a distribuição
    // REQ17: bloqueia se a instituição atingiu/excederia a capacidade mensal
    private String validarERegistrarDistribuicao(List<InstituicaoBeneficiaria> insts, int familias, List<Object[]> itens) {
        LocalDate hoje = LocalDate.now();
        YearMonth mes = YearMonth.from(hoje);

        // REQ17 — capacidade mensal
        for (InstituicaoBeneficiaria inst : insts) {
            int usado = familiasNoMes(inst, mes);
            int cap = inst.getCapacidadeMensal();
            if (usado >= cap) {
                return "A instituição \"" + inst.getNome() + "\" já atingiu a capacidade mensal ("
                        + usado + "/" + cap + " famílias).";
            }
            if (usado + familias > cap) {
                return "Atender " + familias + " famílias excederia a capacidade mensal de \"" + inst.getNome()
                        + "\" (já atendidas: " + usado + " de " + cap + ").";
            }
        }

        // REQ09/13/16/18 — planejamento FIFO só com lotes válidos e com saldo
        IdentityHashMap<LoteEstoque, Double> reserva = new IdentityHashMap<>();
        for (InstituicaoBeneficiaria inst : insts) {
            for (Object[] it : itens) {
                Alimento a = (Alimento) it[0];
                List<Alocacao> aloc = planejarFIFO(a, (Double) it[1], reserva);
                if (aloc == null) {
                    return "Estoque válido insuficiente de \"" + a.getNome() + "\" para atender todas as instituições.\n"
                            + "Lotes vencidos não podem ser distribuídos e a quantidade não pode exceder o estoque.";
                }
            }
        }

        // Tudo validado: a Fachada aplica FIFO, valida de novo e baixa o estoque (uma distribuição por instituição)
        Map<Alimento, Double> pedido = new LinkedHashMap<>();
        for (Object[] it : itens) pedido.merge((Alimento) it[0], (Double) it[1], Double::sum);
        for (InstituicaoBeneficiaria inst : insts) {
            fachada.registrarDistribuicao(inst, new LinkedHashMap<>(pedido), familias, hoje);
        }
        return null;
    }

    // ----- FIFO -----
    private static final class Alocacao {
        final LoteEstoque lote;
        final double qtd;

        Alocacao(LoteEstoque lote, double qtd) {
            this.lote = lote;
            this.qtd = qtd;
        }
    }

    /**
     * Aloca a quantidade nos lotes do alimento, do vencimento mais próximo ao mais distante,
     * ignorando lotes vencidos. Devolve null se o estoque válido não for suficiente
     * (e então não altera o mapa de reservas).
     */
    // REQ09: aplica FIFO: vencimento mais próximo primeiro
    // REQ13: ignora lotes vencidos
    // REQ16: devolve null se o estoque válido não bastar
    // REQ18: ordem sempre por data de validade
    private List<Alocacao> planejarFIFO(Alimento a, double quantidade, IdentityHashMap<LoteEstoque, Double> reserva) {
        IdentityHashMap<LoteEstoque, Double> tmp = new IdentityHashMap<>(reserva);
        List<Alocacao> resultado = new ArrayList<>();
        double restante = quantidade;
        for (LoteEstoque l : lotesFIFO(a)) {
            if (restante <= 1e-9) break;
            if (expirado(l)) continue; // REQ13
            double livre = l.getQuantidadeDisponivel() - tmp.getOrDefault(l, 0.0);
            if (livre <= 1e-9) continue;
            double usar = Math.min(livre, restante);
            resultado.add(new Alocacao(l, usar));
            tmp.merge(l, usar, Double::sum);
            restante -= usar;
        }
        if (restante > 1e-9) return null; // REQ16
        reserva.clear();
        reserva.putAll(tmp);
        return resultado;
    }

    private List<LoteEstoque> lotesFIFO(Alimento a) {
        return fachada.listarEstoque().stream()
                .filter(l -> Objects.equals(l.getAlimento(), a))
                .sorted(Comparator.comparing(LoteEstoque::getDataValidade)
                        .thenComparing(l -> String.valueOf(l.getId())))
                .collect(Collectors.toList());
    }

    private List<LoteEstoque> lotesOrdenados() {
        return fachada.listarEstoque().stream()
                .sorted(Comparator.comparing(LoteEstoque::getDataValidade)
                        .thenComparing(l -> String.valueOf(l.getId())))
                .collect(Collectors.toList());
    }

    private boolean expirado(LoteEstoque l) {
        return l.getDataValidade().isBefore(LocalDate.now());
    }

    private double disponivelValido(Alimento a) {
        return fachada.listarEstoque().stream()
                .filter(l -> Objects.equals(l.getAlimento(), a) && !expirado(l))
                .mapToDouble(LoteEstoque::getQuantidadeDisponivel).sum();
    }

    private List<Alimento> alimentosComEstoqueValido() {
        return fachada.listarAlimentos().stream().filter(a -> disponivelValido(a) > 1e-9).collect(Collectors.toList());
    }

    private int familiasNoMes(InstituicaoBeneficiaria inst, YearMonth mes) {
        return fachada.listarTodasDistribuicoes().stream()
                .filter(d -> Objects.equals(d.getInstituicao(), inst)
                        && YearMonth.from(d.getDataDistribuicao()).equals(mes))
                .mapToInt(Distribuicao::getFamiliasAtendidas).sum();
    }

    /** Os lotes usam UUID; na tela mostramos só os 8 primeiros caracteres. */
    private static String idCurto(LoteEstoque l) {
        String id = String.valueOf(l.getId());
        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    private static String dias(long n) { return n + (n == 1 ? " dia" : " dias"); }

    private String statusLote(LoteEstoque l) {
        if (l.getQuantidadeDisponivel() <= 0) return "Esgotado";
        long d = ChronoUnit.DAYS.between(LocalDate.now(), l.getDataValidade());
        if (d < 0) return "Vencido há " + dias(-d);
        if (d == 0) return "Vence hoje";
        if (d <= 7) return "Vence em " + dias(d);
        return "Válido";
    }

    // =====================================================================
    // 7. RELATÓRIOS  (REQ10, REQ11, REQ12 + bônus)
    // =====================================================================
    // REQ10: relatório de doações em CSV
    // REQ11: relatório nutricional por instituição
    // REQ12: estoque a vencer em 7 dias
    private void abrirTelaRelatorios() {
        JDialog d = new JDialog(this, "Relatórios e Exportação", true);
        d.setSize(560, 600); // 520 cortava a 2ª linha do texto dos cartões
        d.setLocationRelativeTo(this);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(cabecalho("Relatórios e Exportação", "Consulte, exporte e acompanhe o impacto do projeto"), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(3, 1, 0, 14));
        cards.setOpaque(false);
        cards.setBorder(new EmptyBorder(20, 24, 8, 24));
        cards.add(new MenuCard("doacao", "Doações recebidas", "Por período e categoria, exportável em CSV", MenuCard.CLARO, this::abrirRelatorioDoacoes));
        cards.add(new MenuCard("alimento", "Relatório nutricional", "Calorias e proteínas por instituição no mês", MenuCard.GRADIENTE, this::abrirRelatorioNutricional));
        cards.add(new MenuCard("relogio", "Estoque a vencer", "Lotes que vencem nos próximos 7 dias", MenuCard.ESCURO, this::abrirEstoqueAVencer));
        root.add(cards, BorderLayout.CENTER);

        JPanel rod = rodapeBotoes();
        ModernButton fechar = new ModernButton("Fechar", ModernButton.Kind.SECONDARY);
        fechar.addActionListener(e -> d.dispose());
        rod.add(fechar);
        root.add(rod, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setVisible(true);
    }

    // ---- REQ10: doações por período e categoria, CSV ----
    private void abrirRelatorioDoacoes() {
        JDialog d = new JDialog(this, "Relatório de Doações", true);
        d.setSize(980, 620);
        d.setLocationRelativeTo(this);

        LocalDate hoje = LocalDate.now();
        JTextField de = campo(hoje.withDayOfMonth(1).format(DF));
        JTextField ate = campo(hoje.format(DF));
        List<Object> categorias = new ArrayList<>();
        categorias.add("Todas");
        categorias.addAll(Arrays.asList(CategoriaAlimento.values()));
        JComboBox<Object> cat = combo(categorias, o -> o instanceof String ? (String) o : rotulo(o));

        DefaultTableModel m = modeloTabela("Data", "Doador", "Alimento", "Categoria", "Quantidade", "Validade");
        JTable t = criarTabela(m);
        JLabel resumo = new JLabel();
        resumo.setFont(Theme.bodyBold(13));
        resumo.setForeground(Theme.PRIMARY_DARK);
        List<List<String[]>> atuais = new ArrayList<>();
        atuais.add(new ArrayList<>());

        Runnable filtrar = () -> {
            try {
                LocalDate a = parseData(de.getText());
                LocalDate b = parseData(ate.getText());
                if (b.isBefore(a)) { aviso(d, "A data final não pode ser anterior à inicial."); return; }
                atuais.set(0, linhasDoacoes(a, b, cat.getSelectedItem()));
                m.setRowCount(0);
                for (String[] l : atuais.get(0)) m.addRow(new Object[]{l[0], l[1], l[2], l[3], l[4] + " " + l[5], l[6]});
                resumo.setText(atuais.get(0).size() + " item(ns) de doação no período");
            } catch (DateTimeParseException ex) {
                aviso(d, "Datas inválidas. Use o formato dd/mm/aaaa.");
            }
        };

        ModernButton btnFiltrar = new ModernButton("Filtrar", ModernButton.Kind.PRIMARY);
        btnFiltrar.addActionListener(e -> filtrar.run());
        ModernButton btnCsv = new ModernButton("Exportar CSV", ModernButton.Kind.PRIMARY);
        btnCsv.addActionListener(e -> {
            filtrar.run();
            if (atuais.get(0).isEmpty()) { aviso(d, "Não há doações no período para exportar."); return; }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("relatorio_doacoes.csv"));
            if (fc.showSaveDialog(d) != JFileChooser.APPROVE_OPTION) return;
            try {
                CategoriaAlimento filtro = cat.getSelectedItem() instanceof CategoriaAlimento
                        ? (CategoriaAlimento) cat.getSelectedItem() : null;
                new RelatorioService(new NutricaoService()).exportarRelatorioDoacoesCsv(
                        fachada.listarDoacoes(), parseData(de.getText()), parseData(ate.getText()),
                        filtro, fc.getSelectedFile().getAbsolutePath());
                sucesso(d, "Relatório exportado para:\n" + fc.getSelectedFile().getAbsolutePath());
            } catch (IOException | DateTimeParseException ex) {
                erro(d, "Erro ao gerar CSV: " + ex.getMessage());
            }
        });

        JPanel filtros = new JPanel(new GridBagLayout());
        filtros.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 0, 8);
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; filtros.add(rotuloMuted("De"), g);
        g.gridx = 1; g.weightx = 1; filtros.add(de, g);
        g.gridx = 2; g.weightx = 0; filtros.add(rotuloMuted("Até"), g);
        g.gridx = 3; g.weightx = 1; filtros.add(ate, g);
        g.gridx = 4; g.weightx = 0; filtros.add(rotuloMuted("Categoria"), g);
        g.gridx = 5; g.weightx = 1; filtros.add(cat, g);
        g.gridx = 6; g.weightx = 0; filtros.add(btnFiltrar, g);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(cabecalho("Relatório de Doações", "Doações recebidas por período e categoria de alimento"), BorderLayout.NORTH);
        JPanel corpo = new JPanel(new BorderLayout(0, 12));
        corpo.setOpaque(false);
        corpo.setBorder(new EmptyBorder(16, 22, 0, 22));
        corpo.add(filtros, BorderLayout.NORTH);
        corpo.add(rolagem(t), BorderLayout.CENTER);
        corpo.add(resumo, BorderLayout.SOUTH);
        root.add(corpo, BorderLayout.CENTER);

        JPanel rod = rodapeBotoes();
        ModernButton fechar = new ModernButton("Fechar", ModernButton.Kind.SECONDARY);
        fechar.addActionListener(e -> d.dispose());
        rod.add(btnCsv);
        rod.add(fechar);
        root.add(rod, BorderLayout.SOUTH);
        d.setContentPane(root);
        filtrar.run();
        d.setVisible(true);
    }

    private List<String[]> linhasDoacoes(LocalDate de, LocalDate ate, Object categoria) {
        List<String[]> linhas = new ArrayList<>();
        List<Doacao> ordenadas = fachada.listarDoacoes().stream()
                .sorted(Comparator.comparing(Doacao::getDataDoacao)).collect(Collectors.toList());
        for (Doacao dc : ordenadas) {
            if (dc.getDataDoacao().isBefore(de) || dc.getDataDoacao().isAfter(ate)) continue;
            for (ItemDoacao it : dc.getItens()) {
                Alimento a = it.getAlimento();
                if (categoria instanceof CategoriaAlimento && a.getCategoria() != categoria) continue;
                linhas.add(new String[]{DF.format(dc.getDataDoacao()), dc.getDoador().getNome(), a.getNome(),
                        rotulo(a.getCategoria()), qf(it.getQuantidade()), unidadeCurta(a),
                        DF.format(it.getDataValidade())});
            }
        }
        return linhas;
    }

    // ---- REQ11 + bônus: relatório nutricional por instituição ----
    // REQ11: calorias e proteínas distribuídas por instituição no mês
    // BÔNUS: botão que gera o relatório HTML e abre no navegador
    private void abrirRelatorioNutricional() {
        JDialog d = new JDialog(this, "Relatório Nutricional", true);
        d.setSize(980, 720);
        d.setLocationRelativeTo(this);

        GraficoBarras grafico = new GraficoBarras();
        JTextField mesField = campo(YearMonth.now().format(DateTimeFormatter.ofPattern("MM/yyyy")));
        DefaultTableModel m = modeloTabela("Instituição", "Distribuições", "Famílias atendidas", "Calorias", "Proteínas");
        JTable t = criarTabela(m);
        JLabel resumo = new JLabel();
        resumo.setFont(Theme.bodyBold(13));
        resumo.setForeground(Theme.PRIMARY_DARK);
        YearMonth[] mesAtual = {YearMonth.now()};

        Runnable atualizar = () -> {
            YearMonth ym = parseMes(mesField.getText());
            if (ym == null) { aviso(d, "Mês inválido. Use o formato mm/aaaa."); return; }
            mesAtual[0] = ym;
            Map<InstituicaoBeneficiaria, double[]> agg = agregarNutricional(ym);
            m.setRowCount(0);
            double k = 0, p = 0;
            for (Map.Entry<InstituicaoBeneficiaria, double[]> e : agg.entrySet()) {
                double[] v = e.getValue();
                k += v[2];
                p += v[3];
                m.addRow(new Object[]{e.getKey().getNome(), (int) v[0], (int) v[1], nf(v[2]) + " kcal", nf(v[3]) + " g"});
            }
            resumo.setText("Total no mês: " + nf(k) + " kcal  •  " + nf(p) + " g de proteína");
            List<Map.Entry<InstituicaoBeneficiaria, double[]>> ordenado = new ArrayList<>(agg.entrySet());
            ordenado.removeIf(en -> en.getValue()[2] <= 0);
            ordenado.sort((x, y) -> Double.compare(y.getValue()[2], x.getValue()[2]));
            List<String> nomes = new ArrayList<>();
            List<Double> kcals = new ArrayList<>();
            for (Map.Entry<InstituicaoBeneficiaria, double[]> en : ordenado.subList(0, Math.min(6, ordenado.size()))) {
                nomes.add(en.getKey().getNome());
                kcals.add(en.getValue()[2]);
            }
            grafico.dados("Calorias distribuídas por instituição", "kcal", nomes, kcals);
        };

        ModernButton btnAtualizar = new ModernButton("Atualizar", ModernButton.Kind.PRIMARY);
        btnAtualizar.addActionListener(e -> atualizar.run());
        ModernButton btnHtml = new ModernButton("Gerar relatório HTML", ModernButton.Kind.PRIMARY);
        btnHtml.addActionListener(e -> {
            atualizar.run();
            try {
                File f = gerarRelatorioHtml(mesAtual[0]);
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(f.toURI());
                } else {
                    sucesso(d, "Relatório gerado em:\n" + f.getAbsolutePath());
                }
            } catch (Exception ex) {
                erro(d, "Não foi possível gerar o relatório HTML: " + ex.getMessage());
            }
        });

        JPanel filtros = new JPanel(new BorderLayout(10, 0));
        filtros.setOpaque(false);
        JPanel esq = new JPanel(new BorderLayout(10, 0));
        esq.setOpaque(false);
        esq.add(rotuloMuted("Mês (mm/aaaa)"), BorderLayout.WEST);
        esq.add(mesField, BorderLayout.CENTER);
        esq.setPreferredSize(new Dimension(260, 36));
        filtros.add(esq, BorderLayout.WEST);
        filtros.add(btnAtualizar, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(cabecalho("Relatório Nutricional", "Calorias e proteínas distribuídas por instituição no mês"), BorderLayout.NORTH);
        JPanel corpo = new JPanel(new BorderLayout(0, 12));
        corpo.setOpaque(false);
        corpo.setBorder(new EmptyBorder(16, 22, 0, 22));
        corpo.add(filtros, BorderLayout.NORTH);
        RoundPanel cardGrafico = new RoundPanel(Theme.CARD);
        cardGrafico.setLayout(new BorderLayout());
        cardGrafico.add(grafico, BorderLayout.CENTER);
        cardGrafico.setPreferredSize(new Dimension(10, 220));
        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(rolagem(t), BorderLayout.CENTER);
        centro.add(cardGrafico, BorderLayout.SOUTH);
        corpo.add(centro, BorderLayout.CENTER);
        corpo.add(resumo, BorderLayout.SOUTH);
        root.add(corpo, BorderLayout.CENTER);

        JPanel rod = rodapeBotoes();
        ModernButton fechar = new ModernButton("Fechar", ModernButton.Kind.SECONDARY);
        fechar.addActionListener(e -> d.dispose());
        rod.add(btnHtml);
        rod.add(fechar);
        root.add(rod, BorderLayout.SOUTH);
        d.setContentPane(root);
        atualizar.run();
        d.setVisible(true);
    }

    /** {distribuições, famílias, kcal, proteínas} por instituição. */
    private Map<InstituicaoBeneficiaria, double[]> agregarNutricional(YearMonth ym) {
        Map<InstituicaoBeneficiaria, double[]> agg = new LinkedHashMap<>();
        for (InstituicaoBeneficiaria i : fachada.listarInstituicoes()) agg.put(i, new double[4]);
        for (Distribuicao dist : fachada.listarTodasDistribuicoes()) {
            if (!YearMonth.from(dist.getDataDistribuicao()).equals(ym)) continue;
            double[] v = agg.computeIfAbsent(dist.getInstituicao(), k -> new double[4]);
            double[] n = nutriDistribuicao(dist);
            v[0] += 1;
            v[1] += dist.getFamiliasAtendidas();
            v[2] += n[0];
            v[3] += n[1];
        }
        return agg;
    }

    // BÔNUS: relatório HTML estático do impacto nutricional, aberto via Desktop.browse()
    private File gerarRelatorioHtml(YearMonth ym) throws IOException {
        Map<InstituicaoBeneficiaria, double[]> agg = agregarNutricional(ym);
        double k = 0, p = 0;
        StringBuilder linhas = new StringBuilder();
        for (Map.Entry<InstituicaoBeneficiaria, double[]> e : agg.entrySet()) {
            double[] v = e.getValue();
            k += v[2];
            p += v[3];
            linhas.append("<tr><td>").append(esc(e.getKey().getNome())).append("</td><td>")
                    .append((int) v[0]).append("</td><td>").append((int) v[1]).append("</td><td>")
                    .append(nf(v[2])).append(" kcal</td><td>").append(nf(v[3])).append(" g</td></tr>\n");
        }
        String html = "<!DOCTYPE html><html lang='pt-BR'><head><meta charset='UTF-8'>"
                + "<title>Impacto nutricional — AlimentaBem</title><style>"
                + "body{font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;background:#F0F6F7;color:#1F2937;margin:0}"
                + "header{background:linear-gradient(120deg,#2A3550,#00606C 55%,#019391);color:#fff;padding:36px 48px}"
                + "header h1{margin:0;font-size:32px}header p{margin:6px 0 0;color:#BFF5EE}"
                + "main{max-width:960px;margin:32px auto;padding:0 24px}"
                + ".kpis{display:flex;gap:16px;margin-bottom:24px}"
                + ".kpi{flex:1;background:#fff;border:1px solid #D8E6E9;border-radius:16px;padding:20px}"
                + ".kpi b{display:block;font-size:30px;color:#019391}.kpi span{color:#6B7280;font-size:13px}"
                + "table{width:100%;border-collapse:collapse;background:#fff;border-radius:16px;overflow:hidden;border:1px solid #D8E6E9}"
                + "th{background:#2A3550;color:#fff;text-align:left;padding:12px 16px;font-size:13px}"
                + "td{padding:12px 16px;border-top:1px solid #E8F1F2}tr:nth-child(even) td{background:#F3FAFA}"
                + "</style></head><body><header><h1>Impacto nutricional</h1><p>AlimentaBem — "
                + ym.format(DateTimeFormatter.ofPattern("MM/yyyy")) + "</p></header><main>"
                + "<div class='kpis'><div class='kpi'><b>" + nf(k) + "</b><span>kcal distribuídas</span></div>"
                + "<div class='kpi'><b>" + nf(p) + " g</b><span>de proteína distribuídas</span></div>"
                + "<div class='kpi'><b>" + agg.size() + "</b><span>instituições</span></div></div>"
                + "<table><tr><th>Instituição</th><th>Distribuições</th><th>Famílias</th><th>Calorias</th><th>Proteínas</th></tr>"
                + linhas + "</table></main></body></html>";
        File f = File.createTempFile("impacto_nutricional_", ".html");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(html);
        }
        return f;
    }

    // ---- REQ12: estoque a vencer ----
    private List<LoteEstoque> lotesAVencer(int dias) {
        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(dias);
        return lotesOrdenados().stream()
                .filter(l -> l.getQuantidadeDisponivel() > 0
                        && !l.getDataValidade().isBefore(hoje) && !l.getDataValidade().isAfter(limite))
                .collect(Collectors.toList());
    }

    // REQ12: tela de itens de estoque que vencem nos próximos 7 dias
    private void abrirEstoqueAVencer() {
        ListScreen<LoteEstoque> tela = new ListScreen<>("Estoque a Vencer",
                "Lotes com vencimento nos próximos 7 dias",
                new String[]{"Lote", "Alimento", "Validade", "Disponível", "Situação"},
                () -> lotesAVencer(7),
                l -> new Object[]{idCurto(l), l.getAlimento().getNome(), DF.format(l.getDataValidade()),
                        qf(l.getQuantidadeDisponivel()) + " " + unidadeCurta(l.getAlimento()), statusLote(l)},
                860, 500);
        tela.show();
    }

    // =====================================================================
    // NUTRIÇÃO
    // =====================================================================
    /** Item sem dados para o cálculo: sem valor nutricional ou (unidade) sem peso por unidade. */
    private static boolean semDadosNutricionais(Alimento a) {
        return a.getValorNutricional() == null || a.gramasPara(1) == null;
    }

    /** {calorias, proteínas(g)} da quantidade informada (mesma conversão do RelatorioService: Alimento.gramasPara). */
    private static double[] nutri(Alimento a, double qtd) {
        ValorNutricional v = a.getValorNutricional();
        Double g = a.gramasPara(qtd);
        if (v == null || g == null) return new double[]{0, 0};
        return new double[]{v.getCaloriasPor100g() * g / 100.0, v.getProteinasPor100gEmGramas() * g / 100.0};
    }

    /** REQ06 */
    // REQ06: soma calorias e proteínas dos itens de uma doação
    private static double[] nutriDoacao(Doacao d) {
        double k = 0, p = 0;
        for (ItemDoacao it : d.getItens()) {
            double[] n = nutri(it.getAlimento(), it.getQuantidade());
            k += n[0];
            p += n[1];
        }
        return new double[]{k, p};
    }

    private static double[] nutriDistribuicao(Distribuicao d) {
        double k = 0, p = 0;
        for (ItemDistribuicao it : d.getItens()) {
            double[] n = nutri(it.getLote().getAlimento(), it.getQuantidadeRetirada());
            k += n[0];
            p += n[1];
        }
        return new double[]{k, p};
    }

    // =====================================================================
    // INTEGRAÇÕES HTTP (BrasilAPI e USDA) — REQ02, REQ05, REQ14
    // =====================================================================
    private <T> void async(Callable<T> tarefa, Consumer<T> ok, Consumer<Exception> falha) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return tarefa.call(); }

            @Override protected void done() {
                try {
                    ok.accept(get());
                } catch (ExecutionException ex) {
                    Throwable c = ex.getCause();
                    falha.accept(c instanceof Exception ? (Exception) c : new Exception(c));
                } catch (InterruptedException ie) {
                    falha.accept(ie);
                }
            }
        }.execute();
    }

    /** Retorna {rua, bairro, cidade, estado} ou lança exceção se o CEP for inválido/indisponível. */
    // REQ02: completa o endereço via BrasilAPI
    // REQ14: CEP inexistente lança erro e bloqueia o cadastro
    private static String[] consultarCep(String cep) throws Exception {
        HttpRequest rq = HttpRequest.newBuilder(URI.create("https://brasilapi.com.br/api/cep/v1/" + cep))
                .timeout(Duration.ofSeconds(8)).header("Accept", "application/json").GET().build();
        HttpResponse<String> rs;
        try {
            rs = HTTP.send(rq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new Exception("Não foi possível consultar a BrasilAPI. Verifique sua conexão com a internet.");
        }
        if (rs.statusCode() == 404 || rs.statusCode() == 400) {
            throw new Exception("CEP inválido ou não encontrado.");
        }
        if (rs.statusCode() != 200) {
            throw new Exception("A BrasilAPI retornou erro HTTP " + rs.statusCode() + ".");
        }
        String b = rs.body();
        return new String[]{jsonStr(b, "street"), jsonStr(b, "neighborhood"), jsonStr(b, "city"), jsonStr(b, "state")};
    }

    private static final class UsdaResultado {
        Double kcal;
        Double proteina;
        String descricao = "";
    }

    // REQ05: chamada à API USDA FoodData Central (foods/search)
    private static UsdaResultado consultarUsda(String termo, String chave) throws Exception {
        if (termo.isBlank()) throw new Exception("Informe o nome do alimento (em inglês) para buscar.");
        if (chave.isBlank()) throw new Exception("Informe a chave da API (use DEMO_KEY para testes).");
        String url = "https://api.nal.usda.gov/fdc/v1/foods/search?query="
                + URLEncoder.encode(termo, StandardCharsets.UTF_8)
                + "&pageSize=1&dataType=Foundation,SR%20Legacy&api_key="
                + URLEncoder.encode(chave, StandardCharsets.UTF_8);
        HttpRequest rq = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12)).header("Accept", "application/json").GET().build();
        HttpResponse<String> rs;
        try {
            rs = HTTP.send(rq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new Exception("Não foi possível consultar a USDA. Verifique sua conexão com a internet.");
        }
        int s = rs.statusCode();
        if (s == 401 || s == 403) throw new Exception("Chave de API da USDA inválida.");
        if (s == 429) throw new Exception("Limite de requisições da USDA atingido. Tente novamente mais tarde.");
        if (s != 200) throw new Exception("A USDA retornou erro HTTP " + s + ".");
        String body = rs.body();
        UsdaResultado r = new UsdaResultado();
        r.kcal = nutriente(body, "Energy[^\"]*", true);
        r.proteina = nutriente(body, "Protein", false);
        if (r.kcal == null && r.proteina == null) return null;
        r.descricao = jsonStr(body, "description");
        return r;
    }

    private static final Pattern VALOR = Pattern.compile("\"value\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern UNIDADE_KCAL = Pattern.compile("\"unitName\"\\s*:\\s*\"KCAL\"");

    private static Double nutriente(String json, String nomeRegex, boolean exigeKcal) {
        Matcher m = Pattern.compile("\"nutrientName\"\\s*:\\s*\"" + nomeRegex + "\"").matcher(json);
        while (m.find()) {
            int ini = json.lastIndexOf('{', m.start());
            int fim = json.indexOf('}', m.end());
            if (ini < 0 || fim < 0) continue;
            String obj = json.substring(ini, fim);
            if (exigeKcal && !UNIDADE_KCAL.matcher(obj).find()) continue;
            Matcher v = VALOR.matcher(obj);
            if (v.find()) return Double.parseDouble(v.group(1));
        }
        return null;
    }

    private static String jsonStr(String json, String chave) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(chave) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        if (!m.find()) return "";
        String s = m.group(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'u':
                        if (i + 4 < s.length()) {
                            sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                        break;
                    case 'n': sb.append(' '); break;
                    case 't': sb.append(' '); break;
                    default: sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // =====================================================================
    // UTILITÁRIOS DE FORMATAÇÃO / PARSE
    // =====================================================================
    private static String nf(double v) { return String.format(PT_BR, "%,.1f", v); }

    private static String qf(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(PT_BR, "%.2f", v);
    }

    private static String soDigitos(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }

    private static String formatarCep(String cep) {
        String d = soDigitos(cep);
        return d.length() == 8 ? d.substring(0, 5) + "-" + d.substring(5) : cep;
    }

    private static String enderecoTexto(Endereco e) {
        if (e == null) return "—";
        StringBuilder sb = new StringBuilder();
        if (e.getRua() != null && !e.getRua().isBlank()) sb.append(e.getRua());
        if (e.getBairro() != null && !e.getBairro().isBlank()) sb.append(sb.length() > 0 ? ", " : "").append(e.getBairro());
        if (e.getCidade() != null && !e.getCidade().isBlank()) {
            sb.append(sb.length() > 0 ? " — " : "").append(e.getCidade());
            if (e.getEstado() != null && !e.getEstado().isBlank()) sb.append("/").append(e.getEstado());
        }
        return sb.length() == 0 ? "—" : sb.toString();
    }

    /** Converte "12", "12,5" ou "12.5" em número; devolve null para qualquer outra coisa (letras, "NaN", "1e5", "5d"...). */
    private static Double parseNum(String s) {
        if (s == null || !s.trim().matches("\\d+([.,]\\d+)?")) return null;
        try {
            double v = Double.parseDouble(s.trim().replace(',', '.'));
            return Double.isNaN(v) || Double.isInfinite(v) ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Converte número inteiro; devolve null se houver letras, sinais, decimais ou se estourar o limite do int. */
    private static Integer parseInt(String s) {
        if (s == null || !s.trim().matches("\\d+")) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Aceita dd/MM/yyyy ou yyyy-MM-dd. */
    private static LocalDate parseData(String s) {
        String t = s.trim();
        try {
            return LocalDate.parse(t, DF);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(t);
        }
    }

    private static YearMonth parseMes(String s) {
        String t = s.trim();
        try {
            return YearMonth.parse(t, DateTimeFormatter.ofPattern("MM/yyyy"));
        } catch (DateTimeParseException e) {
            try {
                return YearMonth.parse(t);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    private static String csv(String v) {
        String s = v == null ? "" : v;
        return (s.contains(",") || s.contains("\"") || s.contains("\n")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Rótulos amigáveis para os enums. */
    private static String rotulo(Object o) {
        if (o == null) return "—";
        String n = o instanceof Enum ? ((Enum<?>) o).name() : o.toString();
        switch (n) {
            case "GRAOS": return "Grãos";
            case "LATICINIOS": return "Laticínios";
            case "HORTIFRUTI": return "Hortifrúti";
            case "ENLATADOS": return "Enlatados";
            case "QUILOGRAMA": return "Quilograma (kg)";
            case "LITRO": return "Litro (L)";
            case "UNIDADE": return "Unidade";
            default: return n.isEmpty() ? n : n.charAt(0) + n.substring(1).toLowerCase();
        }
    }

    /** "Unidade (50 g)" para alimentos contados em unidades; rótulo normal para kg/L. */
    private static String unidadeComPeso(Alimento a) {
        if (a.getUnidadeMedida() != UnidadeMedida.UNIDADE) return rotulo(a.getUnidadeMedida());
        return a.getPesoPorUnidadeGramas() == null ? "Unidade (sem peso)"
                : "Unidade (" + qf(a.getPesoPorUnidadeGramas()) + " g)";
    }

    private static String unidadeCurta(Alimento a) {
        return a.getUnidadeMedida().getSigla();
    }

    // =====================================================================
    public static void main(String[] args) {
        instalarTema();
        SwingUtilities.invokeLater(() -> new AlimentaBemApp().setVisible(true));
    }
}
