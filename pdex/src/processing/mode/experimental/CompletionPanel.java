/*
 * Copyright (C) 2012-14 Manindra Moharana <me@mkmoharana.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package processing.mode.experimental;
import static processing.mode.experimental.ExperimentalMode.log;
import static processing.mode.experimental.ExperimentalMode.log2;
import static processing.mode.experimental.ExperimentalMode.logE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Painter;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.BadLocationException;

import processing.app.syntax.JEditTextArea;

/**
 * Manages the actual suggestion popup that gets displayed
 * @author Manindra Moharana <me@mkmoharana.com>
 *
 */
public class CompletionPanel {
  
  /**
   * The completion list generated by ASTGenerator
   */
  private JList<CompletionCandidate> completionList;

  /**
   * The popup menu in which the suggestion list is shown
   */
  private JPopupMenu popupMenu;

  /**
   * Partial word which triggered the code completion and which needs to be completed
   */
  private String subWord;

  /**
   * Postion where the completion has to be inserted
   */
  private int insertionPosition;

  private TextArea textarea;

  /**
   * Scroll pane in which the completion list is displayed
   */
  private JScrollPane scrollPane;
  
  protected DebugEditor editor;

  /**
   * Triggers the completion popup
   * @param textarea
   * @param position - insertion position(caret pos)
   * @param subWord - Partial word which triggered the code completion and which needs to be completed
   * @param items - completion candidates
   * @param location - Point location where popup list is to be displayed
   * @param dedit
   */
  public CompletionPanel(final JEditTextArea textarea, int position, String subWord,
                         DefaultListModel<CompletionCandidate> items, final Point location, DebugEditor dedit) {
    this.textarea = (TextArea) textarea;
    editor = dedit;
    this.insertionPosition = position;
    if (subWord.indexOf('.') != -1)
      this.subWord = subWord.substring(subWord.lastIndexOf('.') + 1);
    else
      this.subWord = subWord;
    popupMenu = new JPopupMenu();
    popupMenu.removeAll();
    popupMenu.setOpaque(false);
    popupMenu.setBorder(null);
    scrollPane = new JScrollPane();
    if (UIManager.getLookAndFeel().getID().equals("Nimbus")) {
      UIDefaults defaults = new UIDefaults();
      defaults.put("PopupMenu.contentMargins", new InsetsUIResource(0, 0, 0, 0));
      defaults.put("ScrollPane[Enabled].borderPainter", new Painter<JComponent>() {
        public void paint(Graphics2D g, JComponent t, int w, int h) {}
      });
      popupMenu.putClientProperty("Nimbus.Overrides", defaults);
      scrollPane.putClientProperty("Nimbus.Overrides", defaults);
      scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(Integer.MAX_VALUE, 8));
      scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
      scrollPane.getHorizontalScrollBar().setUI(new CompletionScrollBarUI());
      scrollPane.getVerticalScrollBar().setUI(new CompletionScrollBarUI());
    }
    scrollPane.setViewportView(completionList = createSuggestionList(position, items));
    popupMenu.add(scrollPane, BorderLayout.CENTER);
    popupMenu.setPopupSize(280, setHeight(items.getSize())); //TODO: Eradicate this evil
    this.textarea.errorCheckerService.getASTGenerator()
        .updateJavaDoc((CompletionCandidate) completionList.getSelectedValue());
    textarea.requestFocusInWindow();
    popupMenu.show(textarea, location.x, textarea.getBaseline(0, 0)
        + location.y);
    //log("Suggestion shown: " + System.currentTimeMillis());
  }

  public static class CompletionScrollBarUI extends BasicScrollBarUI {

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle trackBounds) {
      g.setColor((Color) UIManager.get("nimbusBlueGrey"));
      g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
      return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
      return createZeroButton();
    }

    private JButton createZeroButton() {
      JButton jbutton = new JButton();
      jbutton.setPreferredSize(new Dimension(0, 0));
      jbutton.setMinimumSize(new Dimension(0, 0));
      jbutton.setMaximumSize(new Dimension(0, 0));
      return jbutton;
    }
  }

  public boolean isVisible() {
    return popupMenu.isVisible();
  }
  
  public void setVisible(boolean v){
    //log("Pred popup visible.");
    popupMenu.setVisible(v);
  }
  
  private int setHeight(int itemCount){
    FontMetrics fm = textarea.getFontMetrics(textarea.getFont());
    float h = (fm.getHeight() + (fm.getDescent()) * 0.5f) * (itemCount);
    if (scrollPane.getHorizontalScrollBar().isVisible())
      h += scrollPane.getHorizontalScrollBar().getHeight() + fm.getHeight()
          + (fm.getDescent() + fm.getAscent()) * 0.8f;
    // 0.5f and 0.8f scaling give respectable results.
    //log("popup height " + Math.min(250,h) 
    //+ scrollPane.getHorizontalScrollBar().isVisible());
    return Math.min(250, (int) h); // popup menu height
  }
  
  /*TODO: Make width dynamic
   protected int setWidth(){
    if(scrollPane.getVerticalScrollBar().isVisible()) return 280;
    float min = 280;
    FontMetrics fm = textarea.getFontMetrics(textarea.getFont()); 
    for (int i = 0; i < completionList.getModel().getSize(); i++) {
      float h = fm.stringWidth(completionList.getModel().getElementAt(i).toString());
      min = Math.min(min, h);
    }
    min += fm.stringWidth("             ");
    log("popup width " + Math.min(280,min));
    return Math.min(280,(int)min); // popup menu height
  }*/

  /**
   * Created the popup list to be displayed 
   * @param position
   * @param items
   * @return
   */
  private JList<CompletionCandidate> createSuggestionList(final int position,
                                    final DefaultListModel<CompletionCandidate> items) {

    JList<CompletionCandidate> list = new JList<CompletionCandidate>(items);
    //list.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedIndex(0);
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          insertSelection();
          hide();
        }
      }
    });
    list.setCellRenderer(new CustomListRenderer());
    list.setFocusable(false);
    return list;
  }
  
  // possibly defunct
  public boolean updateList(final DefaultListModel<CompletionCandidate> items, String newSubword,
                            final Point location, int position) {
    this.subWord = new String(newSubword);
    if (subWord.indexOf('.') != -1)
      this.subWord = subWord.substring(subWord.lastIndexOf('.') + 1);
    insertionPosition = position;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        scrollPane.getViewport().removeAll();
        completionList.setModel(items);
        completionList.setSelectedIndex(0);
        scrollPane.setViewportView(completionList);
        popupMenu.setPopupSize(popupMenu.getSize().width, setHeight(items.getSize()));
        //log("Suggestion updated" + System.nanoTime());
        textarea.requestFocusInWindow();
        popupMenu.show(textarea, location.x, textarea.getBaseline(0, 0)
            + location.y);
        completionList.validate();
        scrollPane.validate();
        popupMenu.validate();
      }
    });
    return true;
  }

  /**
   * Inserts the CompletionCandidate chosen from the suggestion list
   * 
   * @return
   */
  public boolean insertSelection() {
    if (completionList.getSelectedValue() != null) {
      try {
        // If user types 'abc.', subword becomes '.' and null is returned
        String currentSubword = fetchCurrentSubword();
        int currentSubwordLen = currentSubword == null ? 0 : currentSubword
            .length();
        //logE(currentSubword + " <= subword,len => " + currentSubword.length());
        String selectedSuggestion = ((CompletionCandidate) completionList
            .getSelectedValue()).getCompletionString();
        
        if (currentSubword != null) {
          selectedSuggestion = selectedSuggestion.substring(currentSubwordLen);
        } else {
          currentSubword = "";
        }
        
        logE(subWord + " <= subword, Inserting suggestion=> "
            + selectedSuggestion + " Current sub: " + currentSubword);
        if (currentSubword.length() > 0) {
          textarea.getDocument().remove(insertionPosition - currentSubwordLen,
                                        currentSubwordLen);
        }
        
        textarea.getDocument()
            .insertString(insertionPosition - currentSubwordLen,
                          ((CompletionCandidate) completionList
                              .getSelectedValue()).getCompletionString(), null);
        if (selectedSuggestion.endsWith(")")) {
          if (!selectedSuggestion.endsWith("()")) {
            int x = selectedSuggestion.indexOf('(');
            if (x != -1) {
              //log("X................... " + x);
              textarea.setCaretPosition(insertionPosition + (x + 1));
            }
          }
        } else {
          textarea.setCaretPosition(insertionPosition
              + selectedSuggestion.length());
        }
        //log("Suggestion inserted: " + System.currentTimeMillis());
        return true;
      } catch (BadLocationException e1) {
        e1.printStackTrace();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      hide();
    }
    return false;
  }
  
  private String fetchCurrentSubword() {
    //log("Entering fetchCurrentSubword");
    TextArea ta = editor.ta;
    int off = ta.getCaretPosition();
    //log2("off " + off);
    if (off < 0)
      return null;
    int line = ta.getCaretLine();
    if (line < 0)
      return null;
    String s = ta.getLineText(line);
    //log2("lin " + line);
    //log2(s + " len " + s.length());

    int x = ta.getCaretPosition() - ta.getLineStartOffset(line) - 1, x1 = x - 1;
    if(x >= s.length() || x < 0)
      return null; //TODO: Does this check cause problems? Verify.
    log2(" x char: " + s.charAt(x));
    //int xLS = off - getLineStartNonWhiteSpaceOffset(line);    

    String word = (x < s.length() ? s.charAt(x) : "") + "";
    if (s.trim().length() == 1) {
    //      word = ""
    //          + (keyChar == KeyEvent.CHAR_UNDEFINED ? s.charAt(x - 1) : keyChar);
          //word = (x < s.length()?s.charAt(x):"") + "";
      word = word.trim();
      if (word.endsWith("."))
        word = word.substring(0, word.length() - 1);
      
      return word;
    }
    //log("fetchCurrentSubword 1 " + word);
    if(word.equals(".")) return null; // If user types 'abc.', subword becomes '.'
    //    if (keyChar == KeyEvent.VK_BACK_SPACE || keyChar == KeyEvent.VK_DELETE)
    //      ; // accepted these keys
    //    else if (!(Character.isLetterOrDigit(keyChar) || keyChar == '_' || keyChar == '$'))
    //      return null;
    int i = 0;

    while (true) {
      i++;
      //TODO: currently works on single line only. "a. <new line> b()" won't be detected
      if (x1 >= 0) {
//        if (s.charAt(x1) != ';' && s.charAt(x1) != ',' && s.charAt(x1) != '(')
        if (Character.isLetterOrDigit(s.charAt(x1)) || s.charAt(x1) == '_') {

          word = s.charAt(x1--) + word;

        } else {
          break;
        }
      } else {
        break;
      }
      if (i > 200) {
        // time out!
        break;
      }
    }
    //    if (keyChar != KeyEvent.CHAR_UNDEFINED)
    //log("fetchCurrentSubword 2 " + word);
    if (Character.isDigit(word.charAt(0)))
      return null;
    word = word.trim();
    if (word.endsWith("."))
      word = word.substring(0, word.length() - 1);
    //log("fetchCurrentSubword 3 " + word);
    //showSuggestionLater();
    return word;
    //}
  }

  /**
   * Hide the suggestion list
   */
  public void hide() {
    popupMenu.setVisible(false);
    //log("Suggestion hidden" + System.nanoTime());
    //textarea.errorCheckerService.getASTGenerator().jdocWindowVisible(false);
  }

  /**
   * When up arrow key is pressed, moves the highlighted selection up in the list
   */
  public void moveUp() {
    if (completionList.getSelectedIndex() == 0) {
      scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
      selectIndex(completionList.getModel().getSize() - 1);
      return;
    } else {
      int index = Math.max(completionList.getSelectedIndex() - 1, 0);
      selectIndex(index);
    }
    int step = scrollPane.getVerticalScrollBar().getMaximum()
        / completionList.getModel().getSize();
    scrollPane.getVerticalScrollBar().setValue(scrollPane
                                                   .getVerticalScrollBar()
                                                   .getValue()
                                                   - step);
    textarea.errorCheckerService.getASTGenerator()
        .updateJavaDoc((CompletionCandidate) completionList.getSelectedValue());

  }

  /**
   * When down arrow key is pressed, moves the highlighted selection down in the list
   */
  public void moveDown() {
    if (completionList.getSelectedIndex() == completionList.getModel().getSize() - 1) {
      scrollPane.getVerticalScrollBar().setValue(0);
      selectIndex(0);
      return;
    } else {
      int index = Math.min(completionList.getSelectedIndex() + 1, completionList.getModel()
          .getSize() - 1);
      selectIndex(index);
    }
    textarea.errorCheckerService.getASTGenerator()
        .updateJavaDoc((CompletionCandidate) completionList.getSelectedValue());
    int step = scrollPane.getVerticalScrollBar().getMaximum()
        / completionList.getModel().getSize();
    scrollPane.getVerticalScrollBar().setValue(scrollPane
                                                   .getVerticalScrollBar()
                                                   .getValue()
                                                   + step);
  }

  private void selectIndex(int index) {
    completionList.setSelectedIndex(index);
//      final int position = textarea.getCaretPosition();
//      SwingUtilities.invokeLater(new Runnable() {
//        @Override
//        public void run() {
//          textarea.setCaretPosition(position);
//        };
//      });
  }
  
  
  /**
   * Custom cell renderer to display icons along with the completion candidates 
   * @author Manindra Moharana <me@mkmoharana.com>
   *
   */
  private class CustomListRenderer extends
      javax.swing.DefaultListCellRenderer {
    //protected final ImageIcon classIcon, fieldIcon, methodIcon;    
   
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      JLabel label = (JLabel) super.getListCellRendererComponent(list, value,
                                                                 index,
                                                                 isSelected,
                                                                 cellHasFocus);
      if (value instanceof CompletionCandidate) {
        CompletionCandidate cc = (CompletionCandidate) value;
        switch (cc.getType()) {
        case CompletionCandidate.LOCAL_VAR:
          label.setIcon(editor.dmode.localVarIcon);
          break;
        case CompletionCandidate.LOCAL_FIELD:
        case CompletionCandidate.PREDEF_FIELD:
          label.setIcon(editor.dmode.fieldIcon);
          break;
        case CompletionCandidate.LOCAL_METHOD:
        case CompletionCandidate.PREDEF_METHOD:
          label.setIcon(editor.dmode.methodIcon);
          break;
        case CompletionCandidate.LOCAL_CLASS:
        case CompletionCandidate.PREDEF_CLASS:
          label.setIcon(editor.dmode.classIcon);
          break;

        default:
          log("(CustomListRenderer)Unknown CompletionCandidate type " + cc.getType());
          break;
        }

      }
      else
        log("(CustomListRenderer)Unknown CompletionCandidate object " + value);
      
      return label;
    }
  }
  
}