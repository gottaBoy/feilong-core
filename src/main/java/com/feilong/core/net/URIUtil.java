/*
 * Copyright (C) 2008 feilong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.feilong.core.net;

import static com.feilong.core.Validator.isNullOrEmpty;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feilong.core.CharsetType;
import com.feilong.core.URIComponents;
import com.feilong.core.Validator;
import com.feilong.core.lang.StringUtil;
import com.feilong.tools.slf4j.Slf4jUtil;

/**
 * 处理{@link java.net.URI}(Uniform Resource Locator) {@link java.net.URL}(Uniform Resource Identifier) 等.
 * 
 * <h3>{@link java.net.URI}和 {@link java.net.URL}的区别:</h3>
 * 
 * <blockquote>
 * 
 * <table border="1" cellspacing="0" cellpadding="4" summary="">
 * <tr style="background-color:#ccccff">
 * <th align="left">字段</th>
 * <th align="left">说明</th>
 * </tr>
 * 
 * <tr valign="top">
 * <td>{@link java.net.URI URI}<br>
 * Uniform Resource Identifier</td>
 * <td>统一资源标识符,用来唯一的标识一个资源<br>
 * There are two types of URIs: URLs and URNs. <br>
 * See RFC 1630: Universal Resource Identifiers in WWW: A Unifying Syntax for the Expression of Names and Addresses of Objects on the
 * Network as used in the WWW.</td>
 * </tr>
 * 
 * <tr valign="top" style="background-color:#eeeeff">
 * <td>{@link java.net.URL URL}<br>
 * Uniform Resource Locator</td>
 * <td>统一资源定位器,它是一种具体的URI,即URL可以用来标识一个资源,而且还指明了如何locate这个资源. <br>
 * See RFC 1738: Uniform Resource Locators (URL)</td>
 * </tr>
 * 
 * <tr valign="top">
 * <td>URN<br>
 * uniform resource name</td>
 * <td>统一资源命名,是通过名字来标识资源, <br>
 * 比如mailto:java-net@java.sun.com.</td>
 * </tr>
 * </table>
 * 
 * <p>
 * 也就是说,{@link java.net.URI URI}是以一种抽象的,高层次概念定义统一资源标识,而URL和URN则是具体的资源标识的方式.URL和URN都是一种URI.
 * </p>
 * 
 * <p>
 * 一个{@link URI}实例可以代表绝对的,也可以是相对的,只要它符合URI的语法规则;<br>
 * 而{@link URL}类则不仅符合语义,还包含了定位该资源的信息,<span style="color:red">因此它不能是相对的,schema必须被指定</span>.
 * </p>
 * </blockquote>
 * 
 * <h3>关于 {@link URI} 组成部分:</h3>
 * 
 * <blockquote>
 * <ul>
 * <li>getQuery() 返回此 URI 的已解码的查询组成部分.</li>
 * <li>getRawQuery() 返回此 URI 的原始查询组成部分. URI 的查询组成部分(如果定义了)只包含合法的 URI 字符.</li>
 * </ul>
 * </blockquote>
 * 
 * <h3>关于 URI path parameter(Matrix URIs) <a href="http://www.w3.org/DesignIssues/MatrixURIs.html">MatrixURIs</a>:</h3>
 * 
 * <blockquote>
 * 
 * <p style="color:red">
 * Note: relative Matrix URLs are notgenerally implemented so this is just a theoretical discussion.
 * </p>
 * 
 * <p>
 * A URI path parameter is part of a path segment that occurs after its name. <br>
 * </p>
 * <ul>
 * <li>Path parameters offer a unique opportunity to control the representations of resources</li>
 * <li>Since they can't be manipulated by standard Web forms, they have to be constructed out of band</li>
 * <li>Since they're part of the path, they're sequential, unlike query strings</li>
 * <li>Most importantly, however, their behaviour is not explicitly defined.</li>
 * </ul>
 * 
 * <p>
 * When defining constraints for the syntax of path parameters, we can take these characteristics into account, and define parameters that
 * stack sequentially, and each take multiple values.
 * </p>
 * 
 * <p>
 * In the last paragraph of section 3.3, The URI specification suggests employing the semicolon ;, equals = and comma , characters for this
 * task. Therefore:
 * </p>
 * <blockquote>
 * <table border="1" cellspacing="0" cellpadding="4" summary="">
 * <tr style="background-color:#ccccff">
 * <th align="left">characters</th>
 * <th align="left">说明</th>
 * <th align="left">example</th>
 * </tr>
 * <tr valign="top">
 * <td>semicolon ;</td>
 * <td>will delimit the parameters themselves.<br>
 * That is, anything in a path segment to the right of a semicolon will be treated as a new parameter</td>
 * <td>like this: <span style="color:green">/path/name;param1;p2;p3</span></td>
 * </tr>
 * <tr valign="top" style="background-color:#eeeeff">
 * <td>equals sign =</td>
 * <td>will separate parameter names from their values, should a given parameter take values.<br>
 * That is, within a path parameter, everything to the right of an equals sign is treated as a value,</td>
 * <td>like this: <span style="color:green">param=value;p2</span></td>
 * </tr>
 * <tr valign="top">
 * <td>comma ,</td>
 * <td>will separate individual values passed into a single parameter,</td>
 * <td>like this: <span style="color:green">;param=val1,val2,val3</span></td>
 * </tr>
 * </table>
 * </blockquote>
 * <p>
 * This means that although it may be visually confusing, parameter names can take commas but no equals signs, values can take equals signs
 * but no commas, and no part of the path segment can take semicolons literally. All other sub-delimiters should be percent-encoded. This
 * also means that one's opportunities for self-expression with URI paths are further constrained.
 * </p>
 * 
 * <p>
 * 每一个path片段 可以有可选的 path参数 (也叫 matrix参数),这是在path片段的最后由";"开始的一些字符.每个参数名和值由"="字符分隔,像这样:"/file;p=1",这定义了path片段 "file"有一个 path参数 "p",其值为"1".<br>
 * 这些参数并不常用 — 这得清楚 — 但是它们确实是存在,而且从 Yahoo RESTful API 文档我们能找到很好的理由去使用它们: Matrix参数可以让程序在GET请求中可以获取部分的数据集.参考
 * <a href="https://developer.yahoo.com/social/rest_api_guide/partial-resources.html#paging-collection">数据集的分页</a>
 * .因为matrix参数可以跟任何数据集的URI格式的path片段,它们可以在内部的path片段中被使用.
 * </p>
 * </blockquote>
 * 
 * @author <a href="http://feitianbenyue.iteye.com/">feilong</a>
 * @see java.net.URI
 * @see java.net.URL
 * @see URIComponents
 * @see <a href="http://www.w3.org/DesignIssues/MatrixURIs.html">MatrixURIs</a>
 * @since 1.0.0
 */
public final class URIUtil{

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(URIUtil.class);

    /** Don't let anyone instantiate this class. */
    private URIUtil(){
        //AssertionError不是必须的. 但它可以避免不小心在类的内部调用构造器. 保证该类在任何情况下都不会被实例化.
        //see 《Effective Java》 2nd
        throw new AssertionError("No " + getClass().getName() + " instances for you!");
    }

    /**
     * 直接调用 {@link java.net.URI#create(String)} 创造一个URI,异常信息更丰富.
     * 
     * <h3>{@link URI#URI(String)} 和 {@link URI#create(String)}的区别:</h3>
     * <blockquote>
     * <p>
     * {@link URI#URI(String)},如果String对象的URI违反了RFC 2396的语法规则,将会产生一个 {@link URISyntaxException}.<br>
     * 如果知道URI是有效的,不会产生 {@link URISyntaxException},可以使用静态的 {@link java.net.URI#create(String)}方法
     * </p>
     * </blockquote>
     * 
     * @param uri
     *            the uri
     * @return {@link java.net.URI#URI(String)} <br>
     *         如果 <code>uri</code> 是null,抛出 {@link NullPointerException}<br>
     *         如果 <code>uri</code> 是blank,抛出 {@link IllegalArgumentException}
     * @see java.net.URI#URI(String)
     * @since 1.8.0
     */
    public static URI create(String uri){
        Validate.notBlank(uri, "uri can't be blank!");
        try{
            return URI.create(uri);
        }catch (Exception e){
            throw new URIParseException(Slf4jUtil.format("uri:[{}]", uri), e);
        }
    }

    /**
     * 基于 uri字符串和charset创建 {@link URI}.
     * 
     * @param uriString
     *            the uri string
     * @param charsetType
     *            何种编码,如果是null或者 empty,那么参数部分原样返回,自己去处理兼容性问题<br>
     *            否则会先解码,再加码,因为ie浏览器和chrome 浏览器 url中访问路径 ,带有中文情况下 不一致
     * @return 如果 <code>uriString</code> 是null,抛出 {@link NullPointerException}<br>
     *         如果 <code>uriString</code> 是blank,抛出 {@link IllegalArgumentException}<br>
     *         如果 Exception,抛出 {@link URIParseException}
     * @see URI#create(String)
     * @see #encodeUri(String, String)
     * @see <a
     *      href="http://stackoverflow.com/questions/15004593/java-request-getquerystring-value-different-between-chrome-and-ie-browser">
     *      java-request-getquerystring-value-different-between-chrome-and-ie-browser</a>
     */
    public static URI create(String uriString,String charsetType){
        return create(encodeUri(uriString, charsetType));
    }

    /**
     * 基于 uriString和charset创建 {@link URI}.
     * 
     * @param uriString
     *            the uri string
     * @param charsetType
     *            何种编码,如果是null或者 empty,那么参数部分原样返回,自己去处理兼容性问题<br>
     *            否则会先解码,再加码,因为ie浏览器和chrome 浏览器 url中访问路径 ,带有中文情况下 不一致
     * @return 如果 <code>uriString</code> 是null,抛出 {@link NullPointerException}<br>
     *         如果 <code>uriString</code> 是blank,抛出 {@link IllegalArgumentException}
     * @see <a
     *      href="http://stackoverflow.com/questions/15004593/java-request-getquerystring-value-different-between-chrome-and-ie-browser">
     *      java-request-getquerystring-value-different-between-chrome-and-ie-browser</a>
     * @see URI#create(String)
     * @see ParamUtil#addParameterArrayValueMap(String, Map, String)
     * @since 1.4.0
     */
    public static String encodeUri(String uriString,String charsetType){
        Validate.notBlank(uriString, "uriString can't be null/empty!");
        LOGGER.trace("input uriString:[{}],charsetType:{}", uriString, charsetType);

        if (!hasQueryString(uriString) || isNullOrEmpty(charsetType)){
            return uriString;// 不带参数 一般不需要处理
        }

        Map<String, String[]> safeArrayValueMap = ParamUtil.toSafeArrayValueMap(getQueryString(uriString), charsetType);
        String encodeUrl = ParamUtil.addParameterArrayValueMap(uriString, safeArrayValueMap, charsetType);
        LOGGER.trace("input uriString:[{}],charsetType:[{}],after url:[{}]", uriString, charsetType, encodeUrl);
        return encodeUrl;
    }

    /**
     * 获得不含queryString的path,即链接?前面的连接( <span style="color:red">不包含?</span>).
     *
     * @param uriString
     *            the uri
     * @return 如果 isNullOrEmpty(url),返回 {@link StringUtils#EMPTY}
     * @since 1.8.0 change to default
     */
    static String getFullPathWithoutQueryString(String uriString){
        if (isNullOrEmpty(uriString)){
            return StringUtils.EMPTY;
        }
        // 判断url中是否含有?  XXX 有待严谨
        int index = uriString.indexOf(URIComponents.QUESTIONMARK);
        return index == StringUtils.INDEX_NOT_FOUND ? uriString : uriString.substring(0, index);
    }

    /**
     * 获得queryString.
     * 
     * <h3>示例:</h3>
     * <blockquote>
     * 
     * <pre class="code">
     * URIUtil.getQueryString({@code "http://127.0.0.1/cmens/t-b-f-a-c-s-f-p-g-e-i-o.htm?a=1&a=2"})
     * </pre>
     * 
     * 返回: {@code a=1&a=2}
     * </blockquote>
     * 
     * @param uriString
     *            the uri
     * @return 如果传入的参数 <code>uriString</code> isNullOrEmpty,返回 {@link StringUtils#EMPTY};<br>
     *         如果传入的参数 <code>uriString</code> 不含有?,返回 {@link StringUtils#EMPTY};<br>
     *         否则截取第一个出现的?后面内容返回
     * @since 1.8.0 change to default
     */
    static String getQueryString(String uriString){
        if (isNullOrEmpty(uriString)){
            return StringUtils.EMPTY;
        }
        // 判断url中是否含有?  XXX 有待严谨
        int index = uriString.indexOf(URIComponents.QUESTIONMARK);
        return index == StringUtils.INDEX_NOT_FOUND ? StringUtils.EMPTY : StringUtil.substring(uriString, index + 1);
    }

    /**
     * 是否有queryString.
     *
     * @param uriString
     *            the uri string
     * @return 如果 <code>uriString</code> 是null或者empty,返回 {@link StringUtils#EMPTY}
     * @since 1.8.0 change to private
     */
    // XXX 有待严谨
    private static boolean hasQueryString(String uriString){
        return isNullOrEmpty(uriString) ? false : StringUtils.contains(uriString, URIComponents.QUESTIONMARK);
    }

    // [start] encode/decode

    /**
     * 加码,对参数值进行编码 .
     * 
     * <p style="color:red">
     * 不要用 {@link java.net.URLEncoder} 或者 {@link java.net.URLDecoder}来处理整个URL,一般用来处理参数值.
     * </p>
     * 
     * <p>
     * Translates a string into <code>application/x-www-form-urlencoded</code> format using a specific encoding scheme. This method uses the
     * supplied encoding scheme to obtain the bytes for unsafe characters.
     * </p>
     * 
     * <p>
     * 使用以下规则:
     * </p>
     * 
     * <ul>
     * <li>字母数字字符 "a" 到 "z"、"A" 到 "Z" 和 "0" 到 "9" 保持不变.</li>
     * <li>特殊字符 "."、"-"、"*" 和 "_" 保持不变.</li>
     * <li>空格字符 " " 转换为一个加号 "+".</li>
     * <li>所有其他字符都是不安全的,因此首先使用一些编码机制将它们转换为一个或多个字节.<br>
     * 然后每个字节用一个包含 3 个字符的字符串 "%xy" 表示,其中 xy 为该字节的两位十六进制表示形式.<br>
     * 推荐的编码机制是 UTF-8.<br>
     * 但是,出于兼容性考虑,如果未指定一种编码,则使用相应平台的默认编码.</li>
     * </ul>
     *
     * @param value
     *            the value
     * @param charsetType
     *            charsetType {@link CharsetType}
     * @return 如果 <code>value</code> 是null或者empty,返回 {@link StringUtils#EMPTY}<br>
     *         如果 <code>charsetType</code> 是null或者empty,返回 <code>value</code><br>
     * @see URLEncoder#encode(String, String)
     */
    public static String encode(String value,String charsetType){
        return encodeOrDecode(value, charsetType, true);
    }

    /**
     * 解码,对参数值进行解码.
     * 
     * <p style="color:red">
     * 不要用{@link java.net.URLEncoder} 或者 {@link java.net.URLDecoder}来处理整个URL,一般用来处理参数值.
     * </p>
     * 
     * <p>
     * Decodes a <code>application/x-www-form-urlencoded</code> string using a specific encoding scheme. The supplied encoding is used to
     * determine what characters are represented by any consecutive sequences of the form "<code>%<i>xy</i></code>".
     * </p>
     * 
     * <p>
     * Not doing so may introduce incompatibilites.<br>
     * <em><strong>Note:</strong> 
     * 注:<a href="http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">World Wide Web Consortium Recommendation</a>建议指出,UTF-8应该被使用. 不这样做可能会带来兼容性能.</em>
     * </p>
     * 
     * <h3>URLDecoder: Incomplete trailing escape (%) pattern:</h3>
     * <blockquote>
     * 
     * URLDecoder class throws this exception when last char is "%" sign. <br>
     * If "%" sign comes in middle of string then it won't throw exception.
     * 
     * </blockquote>
     *
     * @param value
     *            需要被解码的值
     * @param charsetType
     *            charsetType {@link CharsetType}
     * @return 如果 <code>value</code> 是null或者empty,返回 {@link StringUtils#EMPTY}<br>
     *         如果 <code>charsetType</code> 是null或者empty,返回 <code>value</code><br>
     * @see <a href="http://dwr.2114559.n2.nabble.com/Exception-URLDecoder-Incomplete-trailing-escape-pattern-td5396332.html">Exception ::
     *      URLDecoder: Incomplete trailing escape (%) pattern</a>
     * @see java.net.URLDecoder#decode(String, String)
     * @see "org.springframework.web.util.UriUtils#decode(String, String)"
     */
    public static String decode(String value,String charsetType){
        return encodeOrDecode(value, charsetType, false);
    }

    /**
     * Encode or decode.
     *
     * @param value
     *            the value
     * @param charsetType
     *            the charset type
     * @param encodeOrDecode
     *            true 为encode,false为 decode
     * @return the string
     * @since 1.6.2
     */
    private static String encodeOrDecode(String value,String charsetType,boolean encodeOrDecode){
        if (isNullOrEmpty(value)){
            return StringUtils.EMPTY;
        }
        try{
            return isNullOrEmpty(charsetType) ? value
                            : (encodeOrDecode ? URLEncoder.encode(value, charsetType) : URLDecoder.decode(value, charsetType));
        }catch (UnsupportedEncodingException e){
            throw new URIParseException(e);
        }
    }

    // [end]
}