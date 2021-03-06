(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('NavbarController', NavbarController);

    NavbarController.$inject = ['$rootScope', '$scope', '$state', 'Auth', 'Principal', 'ProfileService', 'LoginService', 'SwitchUserService', 'dataService', '$log'];

    function NavbarController($rootScope, $scope, $state, Auth, Principal, ProfileService, LoginService, SwitchUserService, dataService, $log) {
        var vm = this;

        vm.isNavbarCollapsed = true;
        vm.isAuthenticated = Principal.isAuthenticated;

        vm.login = login;
        vm.logout = logout;
        vm.switchUser = switchUser;
        vm.cancelSwitchUser = cancelSwitchUser;
        vm.toggleNavbar = toggleNavbar;
        vm.collapseNavbar = collapseNavbar;
        vm.$state = $state;
        $rootScope.wfDefsStatistics = [];

        //in ogni caso questa chiamata viene cachata e non viene richiamata ad ogni caricamento della navbar
        ProfileService.getProfileInfo().then(function(response) {
            $rootScope.inDevelopment = (response.activeProfiles.includes('dev') ? 'true' : 'false');
            //verifico qual è il profilo spring con cui è stata avviata l'app per caricare il corrispondente banner
            $rootScope.app = (response.activeProfiles.includes('oiv') ? 'oiv' : 'cnr');
            vm.swaggerEnabled = response.swaggerEnabled;
        });


        function switchUser() {
            collapseNavbar();
            SwitchUserService.open();
        }

        function cancelSwitchUser() {
            collapseNavbar();
            dataService.authentication.cancelImpersonate().then(function() {
                Principal.authenticate(null);
                Principal.identity(true).then(function(account) {
                    $state.reload();
                });
            })
        }

        function login() {
            collapseNavbar();
            LoginService.open();
        }

        function logout() {
            collapseNavbar();
            Auth.logout();
            $state.go('home');
            $rootScope.wfDefsBootable = []; // TODO la logica e' che gli oggetti non vanno svuotati qui
            $rootScope.wfDefsStatistics = []; // TODO la logica e' che gli oggetti non vanno svuotati qui
            $rootScope.wfDefsAll = []; // TODO la logica e' che gli oggetti non vanno svuotati qui
        }

        function toggleNavbar() {
            vm.isNavbarCollapsed = !vm.isNavbarCollapsed;
        }

        function collapseNavbar() {
            vm.isNavbarCollapsed = true;
        }

        function loadAvailableDefinitions() {
            dataService.definitions.all()
                .then(function(response) {
                    //lista delle Process Definition che l'utente può avviare
                    $rootScope.wfDefsBootable = response.data.bootable;
                    $rootScope.wfDefsBootable.push({
                        key: "all",
                        name: "ALL"
                    });
                    //lista di TUTTE le Process Definition
                    $rootScope.wfDefsAll = response.data.all;
                    $rootScope.wfDefsAll.push({
                        key: "all",
                        name: "ALL"
                    });


        //popolo l'array delle process Definitions di cui l'utente loggato può vedere le statistiche
                    vm.account.authorities.filter(function(authority){
                        if(authority.includes('abilitati') || authority.includes('supervisore')){
                            $rootScope.wfDefsStatistics.push(
                                $rootScope.wfDefsAll.filter(function (el){
                                    if(el.key == authority.split(/[#@]/)[1])
                                        return el;

                                })[0]
                            )
        //                		$rootScope.wfDefsStatistics.push({
        //                            key: authority.split(/[#@]/)[1],
        //                            name: authority.split(/[#@]/)[1]
        //                        });
                        }
                    })
                }, function(response) {
                    $log.error(response);
                });
        }

        loadAvailableDefinitions();
        $scope.$on('authenticationSuccess', function(event, args) {
            $log.info(event);
            $log.info(args);
            loadAvailableDefinitions();
        });

        $scope.$watch(function() {
            return Principal.isAuthenticated();
        }, function() {
            Principal.identity().then(function(account) {
                vm.account = account;
            })
        });
    }
})();