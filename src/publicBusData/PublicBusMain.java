package publicBusData;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PublicBusMain {
    public static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        // 웹 접속을 통해서 버스 정보를 ArrayList에 담아옴.
        ArrayList<BusInfo> busInfoList = null;
        ArrayList<BusInfo> busInfoSelectList = null;
        boolean exitFlag = false;

        while (!exitFlag) {
            System.out.println("1. 웹에서 가져오기, 2. 저장하기, 3. 읽어오기, 4. 수정하기, 5. 삭제하기, 6. 종료");
            System.out.print("선택 >> ");
            int choice = Integer.parseInt(sc.nextLine());

            switch (choice) {
                case 1:
                    busInfoList = webConnection();
                    break;
                case 2:
                    // 공공 데이터를 테이블에 저장함
                    if (busInfoList.size() < 1) {
                        System.out.println("가져올 데이터가 없습니다.");
                        continue;
                    }
                    insertBusInfo(busInfoList);
                    break;
                case 3:
                    busInfoSelectList = selectBusInfo();
                    printBusInfo(busInfoSelectList);
                    break;
                case 4: // 수정
                    int no = updateInputNodeno();
                    if (no != 0){
                        updateBusInfo(no);
                    }
                    break;
                case 5: // 삭제
                    deleteBusInfo();
                    break;
                case 6:
                    System.out.println("종료");
                    exitFlag = true;
                    break;
            }

        } // end of while
        System.out.println("The end");

    } // end of main

    // 사용자가 수정할 nodeno 선택 입력하도록 하는 함수
    public static int updateInputNodeno() {
        ArrayList<BusInfo> tempList = selectBusInfo();
        printBusInfo(tempList);
        System.out.println("수정할 nodeno 선택 >> ");
        int no = Integer.parseInt(sc.nextLine());

        return no;
    }

    public static void updateBusInfo(int no) {
        String sql = "UPDATE BUSINFO SET CURDATE = SYSDATE WHERE NODENO = ?";
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DBUtil.getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, no);
            int value = pstmt.executeUpdate();

            if (value == 1) {
                System.out.println(no + " 수정완료");
            } else {
                System.out.println(no + " 수정실패");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    } // end of updateBusInfo()

    // 버스 정보 출력 함수
    public static void printBusInfo(ArrayList<BusInfo> busInfoSelectList) {
        if (busInfoSelectList.size() < 1) {
            System.out.println("출력할 버스정보가 없습니다.");
            return;
        }
        for (BusInfo data : busInfoSelectList) {
            System.out.println(data.toString());
        }

    }

    // 버스 정보 가져오기
    public static ArrayList<BusInfo> selectBusInfo() {
        ArrayList<BusInfo> busInfoList = null;
        String sql = "select * from businfo";
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = DBUtil.getConnection();
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            busInfoList = new ArrayList<>();
            while (rs.next()) {
                BusInfo busInfo = new BusInfo();
                busInfo.setNodeno(rs.getInt("nodeno"));
                busInfo.setGpslati(rs.getDouble("gpslati"));
                busInfo.setGpslong(rs.getDouble("gpslong"));
                busInfo.setNodeid(rs.getString("nodeid"));
                busInfo.setNodenm(rs.getString("nodenm"));
                busInfo.setCurdate(String.valueOf(rs.getDate("curdate")));
                busInfoList.add(busInfo);
            }
        } catch (SQLException se) {
            System.out.println(se);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException se) {
            }
        }
        return busInfoList;
    }

    // 공공 데이터 삭제하기 > 데이터가 비어있는지 확인 > count 이용
    public static void deleteBusInfo() {
        int count = getCountBusInfo();
        if (count == 0) {
            System.out.println("버스 정보 내역이 없습니다.");
            return;
        }
        String sql = "delete from businfo";
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = DBUtil.getConnection();
            pstmt = con.prepareStatement(sql);
            int i = pstmt.executeUpdate();
            if (i != 0) {
                System.out.println("BusInfo 삭제 완료");
            } else {
                System.out.println("BusInfo 삭제 실패");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    } // end of deleteBusInfo()

    // 테이블에 데이터가 존재하는지를 count로 체크
    public static int getCountBusInfo() {
        int count = 0;
        String sql = "select count(*) as cnt from businfo";
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = DBUtil.getConnection();
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                count = rs.getInt("cnt");
            }

        } catch (SQLException se) {
            System.out.println(se);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException se) {
            }
        }
        return count;
    } // end of getSubjectTotalList()

    // 공공 데이터를 테이블에 저장함
    public static void insertBusInfo(ArrayList<BusInfo> busInfoList) {
        if (busInfoList.size() < 1) {
            System.out.println("입력할 데이터가 없어요");
            return;
        }
        deleteBusInfo(); // 이전 데이터 삭제하고 저장함 (일일이 수정하려면 속도 손해)
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DBUtil.getConnection();
            for (BusInfo data : busInfoList) {
                String sql = "INSERT INTO BUSINFO VALUES (?, ?, ?, ?, ?, null)";
                pstmt = con.prepareStatement(sql);
                pstmt.setInt(1, data.getNodeno());
                pstmt.setDouble(2, data.getGpslati());
                pstmt.setDouble(3, data.getGpslong());
                pstmt.setString(4, data.getNodeid());
                pstmt.setString(5, data.getNodenm());
                int value = pstmt.executeUpdate();
                if (value == 1) {
                    System.out.println(data.getNodenm() + " 정류장 등록완료");
                } else {
                    System.out.println(data.getNodenm() + " 정류장 등록실패");
                }
            } // end of for

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }// end of insertBusInfo

    public static ArrayList<BusInfo> webConnection() {
        ArrayList<BusInfo> list = new ArrayList<>();
        String filePath = "src/key.properties";
        // 1. 요청 url 생성
        StringBuilder urlBuilder = new StringBuilder(
                "http://apis.data.go.kr/1613000/BusSttnInfoInqireService/getSttnNoList");
        try {
            Properties properties = new Properties();
            properties.load(new FileReader(filePath));
            String key = properties.getProperty("key");

            urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8")
                    + key);
            urlBuilder.append("&" + URLEncoder.encode("cityCode", "UTF-8") + "="
                    + URLEncoder.encode("25", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("nodeNm", "UTF-8") + "="
                    + URLEncoder.encode("전통시장", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("nodeNo", "UTF-8") + "="
                    + URLEncoder.encode("44810", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("numOfRows", "UTF-8") + "="
                    + URLEncoder.encode("10", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("pageNo", "UTF-8") + "="
                    + URLEncoder.encode("1", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("_type", "UTF-8") + "="
                    + URLEncoder.encode("xml", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 2.서버주소 Connection con
        URL url = null;
        HttpURLConnection conn = null;
        try {
            url = new URL(urlBuilder.toString()); // 웹서버주소 action
            conn = (HttpURLConnection) url.openConnection(); // 접속요청
            conn.setRequestMethod("GET"); // get방식
            conn.setRequestProperty("Content-type", "application/json");
            System.out.println("Response code: " + conn.getResponseCode());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 3.요청내용을 전송 및 응답 처리
        BufferedReader br = null;
        try {
            // conn.getResponseCode() 서버에서 상태코드를 알려주는 값
            int statusCode = conn.getResponseCode();
            System.out.println(statusCode);
            if (statusCode >= 200 && statusCode <= 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }
            Document doc = parseXML(conn.getInputStream());
            // a. field 태그객체 목록으로 가져온다.
            NodeList descNodes = doc.getElementsByTagName("item");
            // b. Corona19Data List객체 생성

            // c. 각 item 태그의 자식태그에서 정보 가져오기
            for (int i = 0; i < descNodes.getLength(); i++) {
                // item
                Node item = descNodes.item(i);
                BusInfo busInfo = new BusInfo();
                // item 자식태그에 순차적으로 접근
                for (Node node = item.getFirstChild(); node != null; node = node.getNextSibling()) {
                    System.out.println(node.getNodeName() + " : " + node.getTextContent());
                    switch (node.getNodeName()) {
                        case "gpslati":
                            busInfo.setGpslati(Double.parseDouble(node.getTextContent()));
                            break;
                        case "gpslong":
                            busInfo.setGpslong(Double.parseDouble(node.getTextContent()));
                            break;
                        case "nodeid":
                            busInfo.setNodeid(node.getTextContent());
                            break;
                        case "nodenm":
                            busInfo.setNodenm(node.getTextContent());
                            break;
                        case "nodeno":
                            busInfo.setNodeno(Integer.parseInt(node.getTextContent()));
                            break;
                    }
                }
                // d. List객체에 추가
                list.add(busInfo);
            }
            // e.최종확인
            for (BusInfo d : list) {
                System.out.println(d);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    // xml 태그를 Document 객체로 변환
    public static Document parseXML(InputStream inputStream) { // Document >> org.dom 으로 임포트
        DocumentBuilderFactory objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder objDocumentBuilder = null;
        Document doc = null;
        try {
            objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();
            doc = objDocumentBuilder.parse(inputStream);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) { // Simple API for XML e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;

    } // end of parseXML
}
