package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.plain.utils.converter.IConverter;

import javax.annotation.Generated;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

class MpFieldNameResolverTest {


    @Getter
    @Setter
    @ToString
    public static class Client implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * id
         */
        private String id;

        /**
         * 客户端的认证账号
         */
        private String clientId;

        /**
         * 客户端创建日期
         */
        private LocalDateTime clientIdIssuedAt;

        /**
         * 客户端凭证
         */
        private String clientSecret;

        /**
         * 凭证有效期
         */
        private LocalDateTime clientSecretExpiresAt;

        /**
         * 客户端名称
         */
        private String clientName;

        /**
         * 客户端的认证方式
         */
        private String clientAuthenticationMethods;

        /**
         * 授权类型
         */
        private String authorizationGrantTypes;

        /**
         * 授权重定向uri
         */
        private String redirectUris;

        /**
         * 范围
         */
        private String scopes;

        /**
         * 客户端相关 setting
         */
        private ClientSettingsVO clientSettings;

        /**
         * token setting
         */
        private TokenSettingsVO tokenSettings;

    }



    @Getter
    @EqualsAndHashCode
    public static class ClientSettingsVO {

        @JsonCreator
        public ClientSettingsVO(@JsonProperty("requireProofKey") Boolean requireProofKey,
                                @JsonProperty("requireAuthorizationConsent") Boolean requireAuthorizationConsent,
                                @JsonProperty("jwkSetUrl") String jwkSetUrl,
                                @JsonProperty("authenticationSigningAlgorithm") String authenticationSigningAlgorithm){
            this.requireProofKey = requireProofKey;
            this.requireAuthorizationConsent = requireAuthorizationConsent;
            this.jwkSetUrl = jwkSetUrl;
            this.authenticationSigningAlgorithm = authenticationSigningAlgorithm;
        }

        /**
         * 如果客户端在执行授权码授予流时需要提供证明密钥质询和验证器，则设置为true。
         */
        private final Boolean requireProofKey;

        /**
         * 如果客户端请求访问时需要授权同意，则设置为true。这适用于所有交互流（例如authorization_code和device_code）。
         */
        private final Boolean requireAuthorizationConsent;

        /**
         * 设置客户端JSON Web密钥集的URL。
         */
        private final String jwkSetUrl;

        /**
         * 设置必须用于对JWT进行签名的JWS算法，该JWT用于在令牌端点对private_key_JWT和Client_secret_JWT身份验证方法的客户端进行身份验证
         */
        private final String authenticationSigningAlgorithm;

    }

    /**
     * token setting(配置) value object
     */
    @Getter
    @EqualsAndHashCode
    public static class TokenSettingsVO {

        @JsonCreator
        public TokenSettingsVO(@JsonProperty("authorizationCodeTimeToLive") Long authorizationCodeTimeToLive,
                               @JsonProperty("accessTokenTimeToLive") Long accessTokenTimeToLive,
                               @JsonProperty("oauth2TokenFormat")  String oauth2TokenFormat,
                               @JsonProperty("reuseRefreshToken") Boolean reuseRefreshToken,
                               @JsonProperty("refreshTokenTimeToLive") Long refreshTokenTimeToLive,
                               @JsonProperty("idTokenSignatureAlgorithm") String idTokenSignatureAlgorithm){
            this.authorizationCodeTimeToLive = authorizationCodeTimeToLive;
            this.accessTokenTimeToLive = accessTokenTimeToLive;
            this.oauth2TokenFormat = oauth2TokenFormat;
            this.reuseRefreshToken = reuseRefreshToken;
            this.refreshTokenTimeToLive = refreshTokenTimeToLive;
            this.idTokenSignatureAlgorithm = idTokenSignatureAlgorithm;
        }

        /**
         * 设置授权码的生存时间。必须大于 0。建议授权码的最长生存期为10分钟。 单位：分钟
         */
        private final Long authorizationCodeTimeToLive;

        /**
         * 设置访问令牌的生存时间。必须大于持续时间零。单位：分钟
         */
        private final Long accessTokenTimeToLive;

        /**
         * 设置 oauth2 token 格式，有 self-contained，reference 两种格式。
         */
        private final String oauth2TokenFormat;

        /**
         * 如果在返回访问令牌响应时重用刷新令牌，则设置为true，如果发出新的刷新令牌，设置为false。
         */
        private final Boolean reuseRefreshToken;


        /**
         * 设置刷新令牌的生存时间，必须大于0。单位：分钟
         */
        private final Long refreshTokenTimeToLive;

        /**
         * 设置id token 签名算法。
         */
        private final String idTokenSignatureAlgorithm;
    }

    /**
     * Client DTO
     */
    @Data
    @TableName("Client")
    public static class ClientPO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * id
         */
        @TableId("id")
        private String id;

        /**
         * 客户端的认证账号
         */
        @TableField("client_id")
        private String clientId;

        /**
         * 客户端创建日期
         */
        @TableField("client_id_issued_at")
        private LocalDateTime clientIdIssuedAt;

        /**
         * 客户端凭证
         */
        @TableField("client_secret")
        private String clientSecret;

        /**
         * 凭证有效期
         */
        @TableField("client_secret_expires_at")
        private LocalDateTime clientSecretExpiresAt;

        /**
         * 客户端名称
         */
        @TableField("client_name")
        private String clientName;

        /**
         * 客户端的认证方式
         */
        @TableField("client_authentication_methods")
        private String clientAuthenticationMethods;

        /**
         * 授权类型
         */
        @TableField("authorization_grant_types")
        private String authorizationGrantTypes;

        /**
         * 授权重定向uri
         */
        @TableField("redirect_uris")
        private String redirectUris;

        /**
         * 范围
         */
        @TableField("scopes")
        private String scopes;

        /**
         * 客户端相关 setting
         */
        @TableField(value = "client_settings",typeHandler = JacksonTypeHandler.class)
        private ClientSettingsVO clientSettings;

        /**
         * token setting
         */
        @TableField(value = "token_settings",typeHandler = JacksonTypeHandler.class)
        private TokenSettingsVO tokenSettings;
    }

    @Mapper
    public interface IClientAndClientPOConverter extends IConverter<Client, ClientPO> {

    }

    @Generated(
            value = "org.mapstruct.ap.MappingProcessor",
            date = "2025-06-16T18:15:54+0800",
            comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.13 (Oracle Corporation)"
    )
    public class IClientAndClientPOConverterImpl implements IClientAndClientPOConverter {

        @Override
        public ClientPO convert(Client arg0) {
            if ( arg0 == null ) {
                return null;
            }

            ClientPO clientPO = new ClientPO();

            clientPO.setId( arg0.getId() );
            clientPO.setClientId( arg0.getClientId() );
            clientPO.setClientIdIssuedAt( arg0.getClientIdIssuedAt() );
            clientPO.setClientSecret( arg0.getClientSecret() );
            clientPO.setClientSecretExpiresAt( arg0.getClientSecretExpiresAt() );
            clientPO.setClientName( arg0.getClientName() );
            clientPO.setClientAuthenticationMethods( arg0.getClientAuthenticationMethods() );
            clientPO.setAuthorizationGrantTypes( arg0.getAuthorizationGrantTypes() );
            clientPO.setRedirectUris( arg0.getRedirectUris() );
            clientPO.setScopes( arg0.getScopes() );
            clientPO.setClientSettings( arg0.getClientSettings() );
            clientPO.setTokenSettings( arg0.getTokenSettings() );

            return clientPO;
        }

        @Override
        public Client reverse(ClientPO arg0) {
            if ( arg0 == null ) {
                return null;
            }

            Client client = new Client();

            client.setId( arg0.getId() );
            client.setClientId( arg0.getClientId() );
            client.setClientIdIssuedAt( arg0.getClientIdIssuedAt() );
            client.setClientSecret( arg0.getClientSecret() );
            client.setClientSecretExpiresAt( arg0.getClientSecretExpiresAt() );
            client.setClientName( arg0.getClientName() );
            client.setClientAuthenticationMethods( arg0.getClientAuthenticationMethods() );
            client.setAuthorizationGrantTypes( arg0.getAuthorizationGrantTypes() );
            client.setRedirectUris( arg0.getRedirectUris() );
            client.setScopes( arg0.getScopes() );
            client.setClientSettings( arg0.getClientSettings() );
            client.setTokenSettings( arg0.getTokenSettings() );

            return client;
        }

        @Override
        public Collection<ClientPO> convert(Collection<Client> arg0) {
            if ( arg0 == null ) {
                return null;
            }

            Collection<ClientPO> collection = new ArrayList<ClientPO>( arg0.size() );
            for ( Client client : arg0 ) {
                collection.add( convert( client ) );
            }

            return collection;
        }

        @Override
        public Collection<Client> reverse(Collection<ClientPO> arg0) {
            if ( arg0 == null ) {
                return null;
            }

            Collection<Client> collection = new ArrayList<Client>( arg0.size() );
            for ( ClientPO clientPO : arg0 ) {
                collection.add( reverse( clientPO ) );
            }

            return collection;
        }
    }


    @Test
    void resolve() {

        String fieldName = MpFieldNameResolver.resolve(Client::getClientId);
        System.out.println(fieldName);
        System.out.println(MpFieldNameResolver.resolve(Client::getClientIdIssuedAt));
        System.out.println(MpFieldNameResolver.resolve(Client::getClientSecret));
        System.out.println(MpFieldNameResolver.resolve(Client::getClientSecretExpiresAt));
        System.out.println(MpFieldNameResolver.resolve(Client::getClientName));
        System.out.println(MpFieldNameResolver.resolve(Client::getClientAuthenticationMethods));
    }


    @Test
    void test() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, ClientPO.class.getName());
        TableInfoHelper.initTableInfo(assistant, ClientPO.class);
        FieldMappingRegistrar.registerMapping(Client.class, ClientPO.class);
        String dbColumn = FieldMappingRegistry.getColumn(Client.class, MpFieldNameResolver.resolve(Client::getClientId));

        System.out.println(dbColumn);
    }
}