package controller;

import com.teamdev.jxbrowser.browser.Browser;
import conf.JXBrowser;
import util.Util;
import daos.AnimeDAO;
import daos.EpisodioDAO;
import daos.PlayListDAO;
import daos.TemporadaDAO;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import model.Episodio;
import model.PlayList;
import model.Temporada;
import model.Usuario;
import model.Anime;
import view.ViewAnime;

public class ControllerAnime implements ControllerView {
    private ViewAnime viewAnime;
    private ArrayList<Episodio> episodiosIndex;
    private Browser jxbrowser;
    private AnimeDAO animeDAO;
    private Anime animeCarregado;
    private Usuario usuario;

    public ControllerAnime() {
        viewAnime = new ViewAnime();
        animeDAO = new AnimeDAO();
        animeCarregado = Session.getAnime();
        apllyActionsDefault();
    }
    
    @Override
    public void refreshView() {
        viewAnime.dispose();
        viewAnime = new ViewAnime();
        apllyActionsDefault();
    }
    
    private void apllyActionsDefault() {
        addVoltarAction();
        addFavoritarAction();
        addTableAction();
        addSeasonAction();
        addEstrelasAction();
        applyViewDefaults();
    }
    
    @Override
    public void showView() {
        Screen.fullScreen(viewAnime);
    }
    
    @Override
    public void hideView() {
        Screen.visible(viewAnime, false);
    }
    
    @Override
    public void applyViewDefaults() {
        animeCarregado = Session.getAnime();
        
        viewAnime.getJPanel1().setPreferredSize(new Dimension(720, 1150));
        viewAnime.getJScrollPane2().setPreferredSize(new Dimension(400, 950));
        
        // Atualizar Usuário
        usuario = Session.getUsuario();
        viewAnime.getJLusuario().setText(usuario.getName());
        
        viewAnime.getJLanocriacao().setText(String.valueOf(animeCarregado.getAnoCriacao()));
        
        Util.applyTextAreaProperties(viewAnime.getJTextArea1(), viewAnime.getJScrollPane3());
        
        String classIndicativa = "classificacao-" + String.valueOf(animeCarregado.getClassificacaoIndicativa()) + ".png";
        Util.setIcon(viewAnime.getJLclassiInd(), classIndicativa, 40, 40);
        viewAnime.getJLclassiInd().setText("");
        
        int nota = animeDAO.buscaUsuarioAvaliacao(animeCarregado.getId(), usuario.getId());
        atualizaAvaliacao(nota);
        
        PlayList playlist = null;
        if (animeCarregado != null) {
            viewAnime.getJAnimeNome().setText(animeCarregado.getNome());
            viewAnime.getJTextArea1().setText(animeCarregado.getDescricao());
            PlayListDAO playListDAO = new PlayListDAO();
            Optional<PlayList> optionalPlay = playListDAO.buscaPorAnime(animeCarregado.getId()).stream().findFirst();
            if (!optionalPlay.isEmpty()) {
                playlist = optionalPlay.get();
            }
        }
        
        // Abre Player no Spotify
        if (playlist != null) {
            jxbrowser = JXBrowser.openInternalFrame(viewAnime, viewAnime.getJInternalFrame2(), playlist.getLink(), new Dimension(1260, 340));
            viewAnime.getJPanel3().setBorder(null);
        }
        ((BasicInternalFrameUI) viewAnime.getJInternalFrame2().getUI()).setNorthPane(null);
        viewAnime.getJInternalFrame2().setBorder(null);
        viewAnime.getJPanel3().setBorder(null);
        viewAnime.getJPanel2().setBorder(null);
        viewAnime.getJScrollPane2().setBorder(null);

        // Carrega Episodios;
        TemporadaDAO temporadaDAO = new TemporadaDAO();
        List<Temporada> temporadas = temporadaDAO.buscaPorAnime(animeCarregado.getId());
        
        viewAnime.getJCtemporadas().removeAll();
        temporadas.forEach(temporada -> {
            viewAnime.getJCtemporadas().addItem(temporada);
        });
        
        String qtdTemporadas = String.valueOf(temporadas.stream().count()) + " Temporadas";
        viewAnime.getJLqtdTemporada().setText(qtdTemporadas);
        
        updateEpisodios();
        
        // Icon Favorito
        String iconImage = (animeCarregado.isFavorito() ? "favoritoSet.png" : "favorito.png");
        Util.setIcon(viewAnime.getJLfavorito(), iconImage);
    }
    
    @Override
    public JFrame getFrame() {
        return viewAnime;
    }

    private Temporada getSelectedTemp() {
        return ((Temporada) viewAnime.getJCtemporadas().getSelectedItem());
    }
    
    private void updateEpisodios() {
        viewAnime.getJTable1();
        viewAnime.getJTable1().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        viewAnime.getJTable1().setBorder(null);
        viewAnime.getJScrollPane1().setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        TableColumnModel columnModel = viewAnime.getJTable1().getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(140);
        columnModel.getColumn(1).setPreferredWidth(1200);
        columnModel.getColumn(0).setCellRenderer(Util.getNumberEpCellRender());
        columnModel.getColumn(1).setCellRenderer(Util.getDescriptionCellRender());
        viewAnime.getJScrollPane1().setPreferredSize(new Dimension(1340, 600));
        viewAnime.getJTable1().setRowHeight(40);
        
        DefaultTableModel dm = (DefaultTableModel) viewAnime.getJTable1().getModel();
        while (dm.getRowCount() > 0) {
            dm.removeRow(0);
        }

        Temporada tempSelected = getSelectedTemp();
        episodiosIndex = new ArrayList<Episodio>();
        if (tempSelected != null) {
            try {
                EpisodioDAO episodioDAO = new EpisodioDAO();
                List<Episodio> episodios = episodioDAO.buscaPorAnimeTemp(animeCarregado.getId(), tempSelected.getNumero());
                for (int i = 0; i < episodios.size(); i++) {
                    Episodio episodio = episodios.get(i);
                    ArrayList row = new ArrayList();
                    episodiosIndex.add(i, episodio);
                    row.add(episodio.getNumero());
                    row.add(episodio.getDescricao());
                    dm.addRow(row.toArray());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void addVoltarAction() {
        viewAnime.jVoltarMouseClicked(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (jxbrowser != null) {
                    jxbrowser.close();
                }
                Routes.initBackPage();
            }
        });
    }
    
    private void addFavoritarAction() {
        viewAnime.jLfavoritoMouseClicked(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Anime animeSessao = Session.getAnime();
                if (animeSessao.isFavorito()) {
                    animeDAO.removeFavorito(animeSessao.getId(), usuario.getId());
                    viewAnime.getJLfavorito().setIcon(new javax.swing.ImageIcon(getClass().getResource("/static/image/favorito.png"))); // NOI18N
                } else {
                    animeDAO.insertFavorito(animeSessao.getId(), usuario.getId());
                    viewAnime.getJLfavorito().setIcon(new javax.swing.ImageIcon(getClass().getResource("/static/image/favoritoSet.png")));
                }
            }
        });
    }
    
    private void addTableAction() {
        viewAnime.jTable1MouseClicked(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = viewAnime.getJTable1().rowAtPoint(e.getPoint());
                Session.setEpisodio(episodiosIndex.get(row));
                Routes.initController(ControllerVideoPlayer.class);
            }
        });
    }
    
    private void addSeasonAction() {
        viewAnime.jCtemporadasActionPerformed(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEpisodios();
            }
        });
    }
    
    private void addEstrelasAction() {
        viewAnime.jLestrela1MouseEntered(new MouseAdapter(){
            @Override
            public void mouseEntered(MouseEvent e) {
                atualizaAvaliacao(1);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (animeDAO.insertUsuarioAvaliacao(animeCarregado.getId(), usuario.getId(), 1)){
                    int nota = animeDAO.buscaUsuarioAvaliacao(animeCarregado.getId(), usuario.getId());
                    atualizaAvaliacao(nota);
                }
            }
        });
        
        viewAnime.jLestrela2MouseEntered(new MouseAdapter(){
            @Override
            public void mouseEntered(MouseEvent e) {
                atualizaAvaliacao(2);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (animeDAO.insertUsuarioAvaliacao(animeCarregado.getId(), usuario.getId(), 2)){
                    int nota = animeDAO.buscaUsuarioAvaliacao(animeCarregado.getId(), usuario.getId());
                    atualizaAvaliacao(nota);
                }
            }
        });
        
        viewAnime.jLestrela3MouseEntered(new MouseAdapter(){
            @Override
            public void mouseEntered(MouseEvent e) {
                atualizaAvaliacao(3);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (animeDAO.insertUsuarioAvaliacao(animeCarregado.getId(), usuario.getId(), 3)){
                    int nota = animeDAO.buscaUsuarioAvaliacao(animeCarregado.getId(), usuario.getId());
                    atualizaAvaliacao(nota);
                }
            }
        });
        
        viewAnime.jLestrela4MouseEntered(new MouseAdapter(){
            @Override
            public void mouseEntered(MouseEvent e) {
                atualizaAvaliacao(4);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (animeDAO.insertUsuarioAvaliacao(animeCarregado.getId(), usuario.getId(), 4)){
                    int nota = animeDAO.buscaUsuarioAvaliacao(animeCarregado.getId(), usuario.getId());
                    atualizaAvaliacao(nota);
                }
            }
        });
        
        viewAnime.jLestrela5MouseEntered(new MouseAdapter(){
            @Override
            public void mouseEntered(MouseEvent e) {
                atualizaAvaliacao(5);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (animeDAO.insertUsuarioAvaliacao(animeCarregado.getId(), usuario.getId(), 5)){
                    int nota = animeDAO.buscaUsuarioAvaliacao(animeCarregado.getId(), usuario.getId());
                    atualizaAvaliacao(nota);
                }
            }
        });
    }
    
    private void atualizaAvaliacao(int nota) {
        JLabel estrela1 = viewAnime.getJLestrela1();
        JLabel estrela2 = viewAnime.getJLestrela2();
        JLabel estrela3 = viewAnime.getJLestrela3();
        JLabel estrela4 = viewAnime.getJLestrela4();
        JLabel estrela5 = viewAnime.getJLestrela5();
        
        String imageStar = "star.png";
        String imageStarOff = "staroff.png";
        switch(nota) {
            case 0: 
                Util.setIcon(estrela1, imageStarOff);
                Util.setIcon(estrela2, imageStarOff);
                Util.setIcon(estrela3, imageStarOff);
                Util.setIcon(estrela4, imageStarOff);
                Util.setIcon(estrela5, imageStarOff);
                break;
            case 1: 
                Util.setIcon(estrela1, imageStar);
                Util.setIcon(estrela2, imageStarOff);
                Util.setIcon(estrela3, imageStarOff);
                Util.setIcon(estrela4, imageStarOff);
                Util.setIcon(estrela5, imageStarOff);
                break;
            case 2:
                Util.setIcon(estrela1, imageStar);
                Util.setIcon(estrela2, imageStar);
                Util.setIcon(estrela3, imageStarOff);
                Util.setIcon(estrela4, imageStarOff);
                Util.setIcon(estrela5, imageStarOff);
                break;
            case 3:
                Util.setIcon(estrela1, imageStar);
                Util.setIcon(estrela2, imageStar);
                Util.setIcon(estrela3, imageStar);
                Util.setIcon(estrela4, imageStarOff);
                Util.setIcon(estrela5, imageStarOff);
                break;
            case 4:
                Util.setIcon(estrela1, imageStar);
                Util.setIcon(estrela2, imageStar);
                Util.setIcon(estrela3, imageStar);
                Util.setIcon(estrela4, imageStar);
                Util.setIcon(estrela5, imageStarOff);
                break;
            case 5:
                Util.setIcon(estrela1, imageStar);
                Util.setIcon(estrela2, imageStar);
                Util.setIcon(estrela3, imageStar);
                Util.setIcon(estrela4, imageStar);
                Util.setIcon(estrela5, imageStar);
                break;
        }
        
        estrela1.setText("");
        estrela2.setText("");
        estrela3.setText("");
        estrela4.setText("");
        estrela5.setText("");
        
        double avaliacaoGeral = animeDAO.buscaAvaliacaoGeral(animeCarregado.getId(), usuario.getId());
        String descricaoAvaliacao = String.format("%,.1f", avaliacaoGeral);
        viewAnime.getJLavaliacaogeral().setText(descricaoAvaliacao);
    }
}
